import sun.jvm.hotspot.memory.Dictionary

import scala.math._
import scala.util.Random
import scala.util.control.Breaks._

object Lab1 extends App {

  val theBreak = ("----------------------Minimal Task-------------------------------------")
  val thePrime = 14

  def isPrime(n: Int): Boolean = {
    if (n <= 1) false
      else if (n == 2) true
      else !(2 until (n-1)).exists(x => n % x == 0)
  }

  def areaCylinder(r: Float, h: Float): Double = {
    (2 * Pi * r * r) + (h * (2 * Pi * r))
  }

  def numbersListReverse() = {
    val aList = List(14,243,342,43,1)
  aList.reverse
  }

  def uniqueNumbers() ={
    val uniqList = List(12, 34, 12, 2, 1, 4)
    uniqList.toSet.sum
  }

  def getRandomSet() ={
    val list = List(1, 3, 4, 5, 6, 2, 4, 5, 3, 4, 5)
    val random = new Random
    val ran = 3;
    for( a <- 0 until ran){
      print(list(random.nextInt(list.length))+ " ")
    }
  }

  def fibonacci(n: Long): Long = {
    var first = 0
    var second = 1
    var count = 0

    while (count < n) {
      val sum = first + second
      first = second
      second = sum
      count = count + 1
    }
    first
  }

  def sentenceTranslator(): String = {
    val dictionary = Map("mama" -> "mother", "papa" -> "father", "child" -> "children")
    val original = "mama and papa are gone have child"

    original.split(" ").map(word => dictionary.getOrElse(word, word)).mkString(" ")
  }

  def arrangeNumbers(a: Int, b: Int, c: Int): Int = {
    val numbers = List(a, b, c)
    val sorted = (for (num <- numbers) yield num.toString)
      .mkString("")
      .toInt

    if (sorted > 100) sorted else arrangeNumbers(numbers(1), numbers(0), numbers(2))
  }

  def arrangeSmallestNumber(a: Int, b: Int, c: Int): Int = {
    val nums = Array(a, b, c)
    nums.sortWith((a, b) => a.toString + b.toString < b.toString + a.toString)
      .mkString("")
      .toInt
  }

  def rotateLeft() = {
    val list = List(2, 3, 4, 6)
    val n = 3;
    val size = list.size
    (list ++ list).slice(n % size, size + n % size)
  }

  def listRightAngleTriangle(limit: Int): List[(Int, Int, Int)] = {
    for {
      a <- 1 to limit
      b <- a to limit
      c = Math.sqrt(a * a + b * b)
      if (c == c.round)
    } yield (a, b, c.toInt)
  }.toList


  println(theBreak)
  println(s"1. Is $thePrime Prime ?")
  println(isPrime(thePrime))
  println(theBreak)
  println("2. Area of cylinder is:")
  println(areaCylinder(3,4))
  println(theBreak)
  println("3. The reversed list is:")
  println(numbersListReverse())
  println(theBreak)
  println("4. The unique numbers sum from the list: ")
  println(uniqueNumbers())
  println(theBreak)
  println("5. Random set of numbers is :")
  println(getRandomSet())
  println(theBreak)
  println("6. First n numbers of sequence Fibonacci are :")
  println(fibonacci(10))
  println(theBreak)
  println("7. Sentence translated is: ")
  println(sentenceTranslator())
  println(theBreak)
  println("8. Arrange numbers: ")
  println(arrangeSmallestNumber(2, 3, 1))
  println(theBreak)
  println("9. Rotate to the left n position: ")
  println(rotateLeft())
  println(theBreak)
  println("10. All tuples a,b,c: ")
  println(listRightAngleTriangle(20))

}
