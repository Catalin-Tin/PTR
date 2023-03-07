import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy, Kill}


class EchoActor extends Actor with ActorLogging {

  override def receive: Receive = {

    case message => println(s"$message")

  }

  override def postRestart(reason: Throwable): Unit = {
    println(s"Worker $self restarted")
  }
}

class SupervisorActors(numWorkers: Int) extends Actor with ActorLogging {

  var workers = (1 to numWorkers).map(_ => context.actorOf(Props[EchoActor]()))

  override def receive: Receive = {

    case message =>
      println(s"Broadcasting some message to all workers: $message")
      workers.foreach(worker => worker ! message)

  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: Exception => SupervisorStrategy.Restart
  }
}

object Pool extends App {
  val system = ActorSystem("SupervisedPool")

  val supervisor = system.actorOf(Props(new SupervisorActors(numWorkers = 3)), "supervisor")
  var selection = system.actorSelection("/user/supervisor/$b")



  supervisor ! "Hello, all workers!"
  Thread.sleep(1000)
  selection ! Kill
  Thread.sleep(1000)
  supervisor ! "Hello once again, all workers!"


  system.terminate()
}