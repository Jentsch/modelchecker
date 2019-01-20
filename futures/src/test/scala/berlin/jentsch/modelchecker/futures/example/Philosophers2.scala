package berlin.jentsch.modelchecker.futures.example

import ecspec.EcSpec
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future, Promise}

class Philosophers2(n: Int)(implicit ec: ExecutionContext) {

  var room = 0

  val forks: Array[Future[Unit]] =
    Array.fill(n)(Future.successful(()))

  def philosopher(i: Int): Future[Unit] = {
    val (leftPromise, rightPromise) = (Promise[Unit], Promise[Unit])

    Future {
      room += 1
    }.flatMap { _ =>
      val stick = forks(i)
      forks(i) = leftPromise.future
      stick
    }.flatMap { _ =>
      val stick = forks((i + 1) % n)
      forks((i + 1) % n) = rightPromise.future
      stick
    }.map { _ =>
      rightPromise.success(())
      leftPromise.success(())
      room -= 1
    }
  }

  def run: Future[_] =
    philosopher(0).zip(philosopher(1))
}

class Philosophers2Spec extends FlatSpec with EcSpec with Matchers {
  behavior of "Philosophers2"
  they should "not starve" in everyInterleaving { implicit ec =>
    new Philosophers2(3).run will complete
  }
}
