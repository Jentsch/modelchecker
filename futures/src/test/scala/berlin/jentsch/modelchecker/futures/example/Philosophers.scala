package berlin.jentsch.modelchecker.futures.example

import java.util.concurrent.atomic.AtomicReference

import berlin.jentsch.modelchecker.futures.EcSpec
import berlin.jentsch.modelchecker.futures.ExecutionContextOps.uninterrupted
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future, Promise}

class Philosophers(n: Int)(implicit ec: ExecutionContext) {

  type Chopstick = AtomicReference[Future[Unit]]

  val chopsticks: Array[Chopstick] =
    Array.fill(n)(new AtomicReference(Future.successful(())))

  def philosopher(
      firstStick: Chopstick,
      secondStick: Chopstick
  ): Future[Unit] = {
    val (firstPromise, secondPromise) = (Promise[Unit], Promise[Unit])

    for {
      // THINK
      _ <- firstStick.getAndSet(firstPromise.future)
      _ <- secondStick.getAndSet(secondPromise.future)
      // EAT
    } yield {
      secondPromise.success(())
      firstPromise.success(())
    }
  }

  def runDeadLock: Future[_] =
    Future.sequence(List.tabulate(n) { i =>
      philosopher(chopsticks(i), chopsticks((i + 1) % n))
    })

  def runOk: Future[_] =
    uninterrupted {
      Future.sequence(List.tabulate(n) {
        case 0 => philosopher(chopsticks(n - 1), chopsticks(0))
        case i => philosopher(chopsticks(i), chopsticks((i + 1) % n))
      })
    }
}

class PhilosophersSpec extends AnyFlatSpec with EcSpec with Matchers {
  behavior of "Philosophers"

  they should "not starve" in pendingUntilFixed(everyInterleaving {
    implicit ec =>
      new Philosophers(3).runDeadLock will complete
  })

  they should "starve on this specific path" in {
    everyInterleaving(Seq(1, 2, 3, 2, 1, 0)) { implicit ec =>
      new Philosophers(3).runDeadLock willNot complete
    }
  }

  they should "not starve when everything is ok" in everyInterleaving {
    implicit ec =>
      new Philosophers(3).runOk will complete
  }
}
