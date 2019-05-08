package berlin.jentsch.modelchecker.scalaz.example

import berlin.jentsch.modelchecker.scalaz.Interpreter._
import org.scalatest.matchers.Matcher
import org.scalatest.{Assertion, FlatSpec, Matchers}
import scalaz.zio.UIO._
import scalaz.zio.{Semaphore, UIO}

object Philosophers {

  private def philosopher(
      firstStick: Semaphore,
      secondStick: Semaphore
  ): UIO[Unit] =
    succeed((/* THINK */ )) *>
      firstStick.acquire *>
      secondStick.acquire *>
      succeed((/* EAT */ )) *>
      secondStick.release *>
      firstStick.release

  def run(n: Int): UIO[Unit] =
    for {
      sticks <- foreach(in = 0 until n)(_ => Semaphore.make(1))
      _ <- foreachPar(0 until n) { i =>
        philosopher(sticks(i), sticks((i + 1) % n))
      }
    } yield ()

  def runOk(n: Int): UIO[Unit] =
    for {
      sticks <- foreach(in = 0 until n)(_ => Semaphore.make(1))
      _ <- foreachPar(0 until n) {
        case 0 => philosopher(sticks(n - 1), sticks(0))
        case i => philosopher(sticks(i), sticks((i + 1) % n))
      }
    } yield ()
}

class PhilosophersSpec extends FlatSpec with Matchers {
  implicit class Syntax[E, A](results: Set[A]) {
    def could(matcher: Matcher[A]): Assertion =
      atLeast(1, results) should matcher
  }

  behavior of "Philosophers"

  they should "sometimes deadlock in wrong configuration" in {
    notFailing(Philosophers.run(3)) could be(None)
  }

  they should "never deadlock" in {
    all(notFailing(Philosophers.runOk(3))) should not(be(None))
  }
}
