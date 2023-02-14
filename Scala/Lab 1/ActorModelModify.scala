import akka.actor.{Actor, ActorSystem, Props}

class ModifyActor extends Actor {
  def receive = {
    case i: Int => println(i + 1)
    case s: String => println(s.toLowerCase())
    case _ => println ("I don't know how to HANDLE this!")
  }
}

object Main extends App {
  val system = ActorSystem("ModifySystem")
  val modifyActor = system.actorOf(Props[ModifyActor](), name = "modifyActor")

  modifyActor ! 100
  modifyActor ! "Hi FAF "
  modifyActor ! (100, " PTR ")

  Thread.sleep(1000)
  system.terminate()
}



