package berlin.jentsch.modelchecker.futures.example
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import ecspec.EcSpec
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future, Promise}

class Philosophers(n: Int)(implicit ec: ExecutionContext) {

  val room: AtomicInteger = new AtomicInteger

  val forks: Array[AtomicReference[Future[Unit]]] =
    Array.fill(n)(new AtomicReference(Future.successful(())))

  def philosophers: immutable.IndexedSeq[Future[Unit]] = 0 until n map philosopher

  def philosopher(i: Int): Future[Unit] = {
    val (leftPromise, rightPromise) = (Promise[Unit], Promise[Unit])
    for {
      _ <- Future { room.incrementAndGet() }
      // THINK
      _ <- forks(i).getAndSet(leftPromise.future)
      _ <- forks((i + 1) % n).getAndSet(leftPromise.future)
      // EAT
      _ <- Future { room.decrementAndGet() }
    } yield {
      rightPromise.success(())
      leftPromise.success(())
    }

  }

  def run: Future[Unit] =
    Future.sequence(philosophers).map(_ => ())

}

class PhilosophersSpec extends FlatSpec with EcSpec with Matchers {
  behavior of "Philosophers"
  it should "termintate" in everyInterleaving { implicit ec =>
    new Philosophers(2).run will complete
  }
}