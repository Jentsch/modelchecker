package berlin.jentsch.modelchecker.scalaz.example

import berlin.jentsch.modelchecker.scalaz.Interpreter
import org.scalatest.{FlatSpec, Matchers}
import scalaz.zio._

object Philosophers {

  private def philosopher(firstStick: Semaphore,
                          secondStick: Semaphore): IO[Nothing, Unit] =
    IO.succeed((/* THINK */ )) *>
      firstStick.acquire *>
      secondStick.acquire *>
      IO.succeed((/* EAT */ )) *>
      secondStick.release *>
      firstStick.release

  def run(n: Int): IO[Nothing, Unit] =
    for {
      sticks <- IO.foreach(in = 0 until n)(_ => Semaphore.make(1))
      philosophers = 0 until n map { i =>
        philosopher(sticks(i), sticks((i + 1) % n))
      }
      _ <- IO.foreachPar(philosophers)(identity)
    } yield ()

  def runOk(n: Int): IO[Nothing, Unit] =
    for {
      sticks <- IO.foreach(in = 0 until n)(_ => Semaphore.make(1))
      philosophers = 0 until n map {
        case 0 => philosopher(sticks(n - 1), sticks(0))
        case i => philosopher(sticks(i), sticks((i + 1) % n))
      }
      _ <- IO.foreachPar(philosophers)(identity)
    } yield ()
}

class PhilosophersSpec extends FlatSpec with Matchers with RTS {
  behavior of "Philosophers"

  they should "have very few concurrent side effects" in pendingUntilFixed {
    unsafeRun(Interpreter.concurrentEffectsCounter(Philosophers.runOk(3))) should be <= 30
  }
}
