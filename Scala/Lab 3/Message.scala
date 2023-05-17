import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

case class Message(topic: String, content: String)

class MessageBroker(port: Int) {
  private var subscribers: Map[String, List[Socket]] = Map()

  def start(): Unit = {
    val serverSocket = new ServerSocket(port)
    println(s"Message broker started on port $port.")

    while (true) {
      val clientSocket = serverSocket.accept()
      Future {
        handleClient(clientSocket)
      }(ExecutionContext.global)
    }
  }

  private def handleClient(socket: Socket): Unit = {
    val source = Source.fromInputStream(socket.getInputStream)
    val input = source.getLines()

    val command = input.next()
    val arguments = command.split(" ")

    if (arguments.nonEmpty) {
      arguments.head match {
        case "SUBSCRIBE" =>
          if (arguments.length > 1) {
            val topic = arguments(1)
            subscribe(topic, socket)
            println(s"New subscriber: ${socket.getInetAddress}:${socket.getPort} to topic $topic")
          } else {
            println("Invalid SUBSCRIBE command. Usage: SUBSCRIBE <topic>")
          }
        case "PUBLISH" =>
          if (arguments.length > 2) {
            val topic = arguments(1)
            val content = arguments.drop(2).mkString(" ")
            publish(Message(topic, content))
            println(s"Message published to topic $topic")
          } else {
            println("Invalid PUBLISH command. Usage: PUBLISH <topic> <content>")
          }
        case _ =>
          println(s"Unknown command: $command")
      }
    }

  }

  private def subscribe(topic: String, socket: Socket): Unit = synchronized {
    subscribers.get(topic) match {
      case Some(sockets) => subscribers += (topic -> (socket :: sockets))
      case None => subscribers += (topic -> List(socket))
    }
  }

  private def publish(message: Message): Unit = synchronized {
    subscribers.get(message.topic) match {
      case Some(sockets) =>
        sockets.foreach { socket =>
          val outputStream = socket.getOutputStream
          val response = s"Message: ${message.content}"
          outputStream.write(response.getBytes)
        }
      case None =>
    }
  }
}

object MessageBrokerApp extends App {
  val broker = new MessageBroker(2525)
  broker.start()
}

//PUBLISH attendance I will go to university tomorrow
//SUBSCRIBE attendance