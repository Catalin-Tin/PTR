import akka.actor.{Actor, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.routing.RoundRobinPool
import akka.stream._
import akka.stream.scaladsl._

import scala.collection.mutable.HashMap
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.matching.Regex

object SSESentiment extends App with EventStreamUnmarshalling {

  implicit val system = ActorSystem("SSEExample")
  implicit val materializer = ActorMaterializer()

  val printerSupervisorStrategy = OneForOneStrategy() {
    case _: Exception => SupervisorStrategy.Restart
  }

  // Define a backoff options for the printer actors, including the supervisor strategy
  val printerBackoffOptions = Backoff.onFailure(
    Props[PrinterActor],
    "PrinterActor",
    1.second,
    30.seconds,
    0.2
  ).withManualReset.withSupervisorStrategy(printerSupervisorStrategy)

  // Define a pool of printer actors supervised by a backoff supervisor
  val printerActorPool = system.actorOf(
    BackoffSupervisor.props(printerBackoffOptions)
      .withRouter(new RoundRobinPool(3)),
    "PrinterActorPool"
  )

  val ReaderActor = Source
    .single(Uri("http://localhost:4000/tweets/1"))
    .map(uri => HttpRequest(uri = uri))
    .mapAsync(1)(Http().singleRequest(_))
    .flatMapConcat(_.entity.dataBytes)
    .map(_.decodeString("UTF-8"))
    .map(ServerSentEvent(_))
    .map(e => (System.nanoTime(), e.data))
    .runForeach {
      case (id, tweetText) =>
        // Send tweet text and id to the printer actor pool
        printerActorPool ! PrinterActorPool.Print(id, tweetText)
    }

  val badWords = Set("Whore", "whore", "Ass", "ass", "Arse", "arse", "Bloody", "bloody", "Bugger", "bugger", "Cow", "cow", "Crap", "crap", "Damn", "damn", "Ginger", "ginger", "Git", "git", "God", "GOD", "god", "Goddam", "goddam", "Jesus Christ", "Minger", "minger", "jesus", "Sod-off", "sod-off", "Arsehole", "arsehole", "Balls", "balls", "Bint", "bint", "Bitch", "bitch", "Bollocks", "bollocks", "bullshit", "Bullshit", "Feck", "feck", "Munter", "munter", "Pissed", "pissed", "pissed off", "Shit", "shit", "Son of a bitch", "son of a bitch", "Tits", "tits", "tit", "Bastard", "bastard", "Beaver", "beaver", "Beef curtains", "beer curtains", "Bellend", "bellend", "Bloodclaat", "bloodclaat", "Clunge", "clunge", "Cock", "cock", "Dick", "dick", "Dickhead", "dickhead", "Fanny", "fanny", "Flaps", "flaps", "Gash", "gash", "Knob", "knob", "Minge", "minge", "Prick", "prick", "Punani", "punani", "Pussy", "pussy", "Snatch", "snatch", "Twat", "twat", "Cunt", "cunt", "Fuck", "fuck", "fucking", "Fucking", "Motherfucker", "motherfucker", "Bonk", "bonk", "Shag", "shag", "Slapper", "slapper", "Tart", "tart")
  val asterisks = "*"

  def replaceBadWords(word: String): String = asterisks * word.length

  case class UpdateEngagementRatio(user: String, engagementRatio: Float)

  class EngagementRatioActor extends Actor {
    var userEngagementRatios: HashMap[String, (Float, Int)] = HashMap.empty

    // Schedule a message to be sent to the actor every minute
    val cancellable = context.system.scheduler.schedule(
      0 seconds,
      10 seconds,
      self,
      "PrintEngagementRatios"
    )

    override def postStop(): Unit = {
      cancellable.cancel()
    }

    override def receive: Receive = {
      case UpdateEngagementRatio(user, engagementRatio) =>
        val (totalEngagementRatio, tweetCount) = userEngagementRatios.getOrElse(user, (0f, 0))
        userEngagementRatios.put(user, (totalEngagementRatio + engagementRatio, tweetCount + 1))

      case "PrintEngagementRatios" =>
        println("Engagement Ratios:")
        userEngagementRatios.foreach { case (user, (totalEngagementRatio, tweetCount)) =>
          val averageEngagementRatio = totalEngagementRatio / tweetCount
          println(s"$user - Total Tweets: $tweetCount, Average Engagement Ratio: $averageEngagementRatio")
        }
        // Reset the tweet count and total engagement ratio for each user
        userEngagementRatios = HashMap.empty
    }
  }

  case object CalculateEngagementRatio

  class PrinterActor extends Actor {
    val tweetRegex: Regex = """(?s).*"text":"(.*?)".*""".r
    val favCountRegex: Regex = """(?s).*"favourites_count":(\d+).*""".r
    val retweetCountRegex: Regex = """(?s).*"retweet_count":(\d+).*""".r
    val followersCountRegex: Regex = """(?s).*"followers_count":(\d+).*""".r
    val userNameRegex: Regex = """(?s).*"user":\{"screen_name":"(.*?)".*""".r

    var id: Long = 0L
    var engagementRatios = Map.empty[String, (Int, Float)]
    implicit val materializer: ActorMaterializer = ActorMaterializer()(context)
    val http = Http(context.system)

    override def preStart(): Unit = {
      context.system.scheduler.scheduleWithFixedDelay(10.seconds, 10.seconds, self, CalculateEngagementRatio)
    }

    override def receive: Receive = {
      case PrinterActorPool.Print(tweetId, tweetJson) =>
        id = tweetId
        tweetRegex.findFirstMatchIn(tweetJson) match {
          case Some(m) =>
            val tweetText = m.group(1).replaceAll("\\\\/", "/")
            val filteredTweet = badWords.foldLeft(tweetText) { (text, word) =>
              text.replaceAll(word, replaceBadWords(word))
            }

            val jsonFields = tweetJson.replaceAll("[\n\r]", "")
            val userFollowersCount = followersCountRegex.findFirstMatchIn(jsonFields).map(_.group(1).toInt).getOrElse(0)
            val tweetFavCount = favCountRegex.findFirstMatchIn(jsonFields).map(_.group(1).toInt).getOrElse(0)
            val tweetRetweetCount = retweetCountRegex.findFirstMatchIn(jsonFields).map(_.group(1).toInt).getOrElse(0)
            val engagementRatio = if (userFollowersCount > 0) (tweetFavCount + tweetRetweetCount) / userFollowersCount.toFloat else 0
            val userName = userNameRegex.findFirstMatchIn(jsonFields).map(_.group(1)).getOrElse("Unknown User")
            engagementRatios.get(userName) match {
              case Some((count, totalRatio)) =>
                val newCount = count + 1
                val newTotalRatio = totalRatio + engagementRatio
                engagementRatios = engagementRatios + (userName -> (newCount, newTotalRatio))
              case None =>
                engagementRatios = engagementRatios + (userName -> (1, engagementRatio))
            }

            val sentimentScore = calculateSentimentScore(filteredTweet)
            println(s"\nText: $filteredTweet\nSentiment Score: $sentimentScore\nEngagement Ratio: $engagementRatio")
          case None =>
            // Kill the actor and restart it
            context.parent ! BackoffSupervisor.reset
            println(s"\nPrintActor $id was killed")
        }
      case CalculateEngagementRatio =>
        engagementRatios.foreach { case (userName, (count, totalRatio)) =>
          val avgEngagementRatio = totalRatio / count
          println(s"\nUser: $userName\nNumber of Tweets: $count\nAverage Engagement Ratio: $avgEngagementRatio")
        }
        engagementRatios = Map.empty[String, (Int, Float)]
    }
  }

  def calculateSentimentScore(tweetText: String)(implicit system: ActorSystem): Int = {
      val emotionValuesUrl = "http://localhost:4000/emotion_values"

      val request = HttpRequest(uri = emotionValuesUrl)
      val responseFuture = Http().singleRequest(request)

      val response = Await.result(responseFuture, Duration.Inf)
      val emotionValues = Await.result(Unmarshal(response.entity).to[String], Duration.Inf)

      val wordToValueMap = emotionValues
        .linesIterator
        .map(line => {
          val pair = line.split('\t')
          pair(0).toLowerCase -> pair(1).toInt
        })
        .toMap

      tweetText.split("\\s+")
        .foldLeft(0) { (score, word) =>
          val wordScore = wordToValueMap.getOrElse(word.toLowerCase, 0)
          score + wordScore
        }
    }
  }



  object PrinterActorPool {
    case class Print(id: Long, tweetText: String)
  }
