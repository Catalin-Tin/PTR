# Topic:  Functional Programming / The Actor Model

### Course: Real Time Programming (PTR)
### Author: Tincu Catalin FAF-201

----
## P0W1 – Welcome..
Followed the installation guide to install the language, wrote a script that would print the message "Hello PTR", created a testing unit and wrote a comprehensive readme for repository.
```
  def inputWord():String ={

  return("Hello PTRT")
  }

  def testString(test: String):Boolean = {

   if (test == "Hello PTR")  true
   else false
  }
```
## P0W2 – ..to the rice fields
1. Prime problem was solved using if conditions, basically checks how many divisors the number has.
```
 def isPrime(n: Int): Boolean = {
    if (n <= 1) false
      else if (n == 2) true
      else !(2 until (n-1)).exists(x => n % x == 0)
  }
```
2. Area of cylinder problem was solved using simple arithmetic operations.
```
def areaCylinder(r: Float, h: Float): Double = {
    (2 * Pi * r * r) + (h * (2 * Pi * r))
  }
```
3. Reverse list problem was solved using .reverse method
```
 def numbersListReverse() = {
    val aList = List(14,243,342,43,1)
  aList.reverse
  }
```
4. Sum of unique numbers from the list  was solved using .toSed and .sum methods.
```
 def uniqueNumbers() ={
    val uniqList = List(12, 34, 12, 2, 1, 4)
    uniqList.toSet.sum
  }
```
5. Random selected numbers from the list was done using random.nextInt method combined with the length of the list, variable ran represents the number of random numbers that should be extracted. 
```
 def getRandomSet() ={
    val list = List(1, 3, 4, 5, 6, 2, 4, 5, 3, 4, 5)
    val random = new Random
    val ran = 3;
    for( a <- 0 until ran){
      print(list(random.nextInt(list.length))+ " ")
    }
  }
```
6. First n elements of Fibonacci sequence was done using while condition which basically adds the first two numbers, increments the count and sums the previous number with the next one.
```
 while (count < n) {
      val sum = first + second
      first = second
      second = sum
      count = count + 1
    }
```
7. Dictionary which translates a sentence was done using Map that matches the words that should be replaced, split method is used to split the text into array of words, getOrElse is used to either change the word from dictionary or retrieve the same word if it didn't get a match and mkString translates the words back into sentence.
```
    val dictionary = Map("mama" -> "mother", "papa" -> "father", "child" -> "children")
    val original = "mama and papa are gone have child"

    original.split(" ").map(word => dictionary.getOrElse(word, word)).mkString(" ")
```
8. Smallest possible number was done using mkString, toInt, and also I have an if condition to check if the number is less than 100, it means that 0 is in front and I change manually the second number with the first to obtain the smallest number which doesn't start with 0.
```
   val sorted = (for (num <- numbers) yield num.toString)
      .mkString("")
      .toInt

    if (sorted > 100) sorted else arrangeNumbers(numbers(1), numbers(0), numbers(2))
```
9. Rotate the list to the left n places was done using list.size and slice that selects an interval of elements.
```
  def rotateLeft() = {
    val list = List(2, 3, 4, 6)
    val n = 3;
    val size = list.size
    (list ++ list).slice(n % size, size + n % size)
  }

```
10. List of all tuples was done using sqrt and round which are basic mathematical methods, also with toList I make a list with all the numbers that checks the condition. 
```
  def listRightAngleTriangle(limit: Int): List[(Int, Int, Int)] = {
    for {
      a <- 1 to limit
      b <- a to limit
      c = Math.sqrt(a * a + b * b)
      if (c == c.round)
    } yield (a, b, c.toInt)
  }.toList
```
## P0W3 – An Actor is Born
1. Actor that prints any messages that it gets project has 1 class with case message that just prints something on the screen
```
class PrintActor  extends Actor {

  def receive = {
    case message => println(message)

  }
```
System creating and actor generating (this is basically the same as in all the actors projects, I will show an example below of system, actor creating and input, since in each project it's basically the same structure I will not mention them again).
```
  val system = ActorSystem("SimpleSystem")
  val actor = system.actorOf(Props[PrintActor](), "PrintActor")
```
input
```
  actor ! 235
  actor ! "Hi FAF"
  actor ! Map {300 -> "PTR"}
```
2. Actor that prints the modified message project has 1 class with 3 cases, one for the numbers that should be modified by 1, string that should be changed from uppercase to lowercase using LowerCase method and last case that describes all the other cases.
```
class ModifyActor extends Actor {
  def receive = {
    case i: Int => println(i + 1)
    case s: String => println(s.toLowerCase())
    case _ => println ("I don't know how to HANDLE this!")
  }
}
```
3. Actor that monitors and another actor that is being monitored project hs 2 classes:

a. One class was reused from the first project of the week, that just prints the message
```
class MonitoredActor extends Actor {
  def receive = {
    case message => println(message)
  }
}
```

b. Second class monitors the first class and has 2 cases, first one being the termination point that prints actor has stopped when the first actor doesn't print anything and second case is when monitored actor is preparing some task.

```
class MonitoringActor(monitoredActor: ActorRef) extends Actor {
  context.watch(monitoredActor)

  def receive = {
    case Terminated(actor) if actor == monitoredActor =>
        println("Monitored actor has stopped!")
    case _ => println("Task is being prepared")
  }
}
```
4. Actor prints average of given numbers project has 1 class that has 1 case Addnumber where average is being counted.
```
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
```
## P0W4 – The Actor is dead.. Long live the Actor
Create a supervised pool of identical worker actors. The number of actors is static, given at initialization. Workers should be individually addressable. Worker actors should echo any message they receive. If an actor dies (by receiving a “kill” message), it should be restarted by the supervisor. Logging is welcome.
1. First class called EchoActor prints the message that every actor gets and when the server restarts
```
  override def receive: Receive = {
    case message => println(s"$message")
  }

  override def postRestart(reason: Throwable): Unit = {
    println(s"Worker $self restarted")
  }
```
2. SupervisorActors class distributes message to every actor using oneforonestrategy and sets exception for restart 
```
  override def receive: Receive = {
    case message =>
      println(s"Broadcasting some message to all workers: $message")
      workers.foreach(worker => worker ! message)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: Exception => SupervisorStrategy.Restart
  }
```
