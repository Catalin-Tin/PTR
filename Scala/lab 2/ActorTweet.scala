import akka.actor.{Actor, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.routing.RoundRobinPool
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.util.matching.Regex

object SSEExample extends App with EventStreamUnmarshalling {

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

  class PrinterActor extends Actor {
    val tweetRegex: Regex = """(?s).*"text":"(.*?)".*""".r
    var id: Long = 0L

    override def receive: Receive = {
      case PrinterActorPool.Print(tweetId, tweetText) =>
        id = tweetId
        tweetRegex.findFirstMatchIn(tweetText) match {
          case Some(m) =>
            val tweet = m.group(1).replaceAll("\\\\/", "/")
            val tweetText = tweet.split("\\s+").filterNot(_.startsWith("@")).mkString(" ")
            println(s"\nText: $tweetText")
          case None =>
            // Kill the actor and restart it
            context.parent ! BackoffSupervisor.reset
            println(s"\n PrintActor $id was killed")
        }
    }
  }

  object PrinterActorPool {
    case class Print(id: Long, tweetText: String)
  }
}
