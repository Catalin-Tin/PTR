import akka.actor.{Actor, ActorSystem, Props, Terminated, ActorRef}

class MonitoredActor extends Actor {
  def receive = {
    case message => println(message)
  }
}

class MonitoringActor(monitoredActor: ActorRef) extends Actor {
  context.watch(monitoredActor)

  def receive = {
    case Terminated(actor) if actor == monitoredActor =>
        println("Monitored actor has stopped!")
    case _ => println("Task is being prepared")
  }
}

object Starting extends App {
  val system = ActorSystem("MonitoringSystem")

  val monitoredActor = system.actorOf(Props[MonitoredActor](), name = "monitoredActor")
  val monitoringActor = system.actorOf(Props(new MonitoringActor(monitoredActor)), name = "monitoringActor")

  monitoringActor ! "Task I"
  monitoringActor ! "Task II"
  monitoringActor ! 333

  Thread.sleep(1000)

  system.stop(monitoredActor)

  Thread.sleep(1000)

  system.terminate()
}