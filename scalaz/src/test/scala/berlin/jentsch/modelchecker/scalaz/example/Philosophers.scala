package berlin.jentsch.modelchecker.scalaz.example

import org.scalatest.{FlatSpec, Matchers}
import scalaz.zio.{IO, Semaphore}

object Philosophers {

  private def philosopher(firstStick: Semaphore,
                  secondStick: Semaphore): IO[Nothing, Unit] =
    IO.now((/* THINK */ )) *>
      firstStick.acquire *>
      secondStick.acquire *>
      IO.now((/* EAT */ )) *>
      secondStick.release *>
      firstStick.release

  def apply(n: Int): IO[Nothing, Unit] =
    for {
      sticks <- IO.sequence(List.fill(n)(Semaphore(0)))
      philosophers = 0 until n map { i =>
        philosopher(sticks(i), sticks((i + 1) % n))
      }
      _ <- IO.parAll(philosophers)
    } yield ()
}

class PhilosophersSpec extends FlatSpec with Matchers {
  behavior of "Philosophers"
//  they should "not starve" in {
//      everyInterleavingOf(Philosophers(2).run) will complete
//  }
}
