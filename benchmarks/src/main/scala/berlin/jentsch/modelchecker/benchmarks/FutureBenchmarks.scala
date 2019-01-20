package berlin.jentsch.modelchecker.benchmarks

import java.util.concurrent.TimeUnit

import ecspec.EcSpec
import org.openjdk.jmh.annotations._
import org.scalatest.{FlatSpec, Matchers}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.SECONDS)
@Measurement(time = 5, iterations = 3)
@Warmup(time = 5, iterations = 2)
class FutureBenchmarks extends FlatSpec with Matchers with EcSpec {

  @Benchmark
  def speed: Unit = everyInterleaving { implicit ec =>
    new Philosophers(3).runOk will complete
  }
}


import java.util.concurrent.atomic.AtomicReference

import ecspec.EcSpec
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future, Promise}

class Philosophers(n: Int)(implicit ec: ExecutionContext) {

  type Chopstick = AtomicReference[Future[Unit]]

  val chopsticks: Array[Chopstick] =
    Array.fill(n)(new AtomicReference(Future.successful(())))

  def philosopher(firstStick: Chopstick, secondStick: Chopstick): Future[Unit] = {
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
    Future.sequence(List.tabulate(n){ i =>
      philosopher(chopsticks(i), chopsticks((i + 1) % n))
    })

  def runOk: Future[_] =
    Future.sequence(List.tabulate(n - 1){ i =>
      philosopher(chopsticks(i), chopsticks((i + 1) % n))
    })
}

class PhilosophersSpec extends FlatSpec with EcSpec with Matchers {
  behavior of "Philosophers"

  they should "not starve" in pendingUntilFixed(everyInterleaving { implicit ec =>
    new Philosophers(3).runDeadLock will complete
  })

  they should "not starve when everything is ok" in everyInterleaving { implicit ec =>
    new Philosophers(3).runOk will complete
  }
}
