import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props

case class AddNumber(num: Double)

class TotalActor extends Actor {
  private var total:Double = 0
  private var counter = 0

  def receive: Receive = {
    case AddNumber(num) => {
      counter += 1
      total += num
      total /= counter
    }
      println("Average of numbers is:" + total)
  }
}

object Startup {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("MySystem")
    val totalActor = system.actorOf(Props[TotalActor](), name = "totalActor")

    // Add numbers and print running total
    totalActor ! AddNumber(8)
    totalActor ! AddNumber(10)
    totalActor ! AddNumber(5)
  }
}
