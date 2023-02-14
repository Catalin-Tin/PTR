import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props

class PrintActor  extends Actor {

  def receive = {
    case message => println(message)

  }

}
object Start extends App {
  val system = ActorSystem("SimpleSystem")
  val actor = system.actorOf(Props[PrintActor](), "PrintActor")

  actor ! 235
  actor ! "Hi FAF"
  actor ! Map {300 -> "PTR"}

}