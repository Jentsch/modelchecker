package berlin.jentsch.modelchecker.scalaz
import org.scalatest.{FlatSpec, Matchers}
import scalaz.zio.{Ref, UIO}

class InterpreterSpec extends FlatSpec with Matchers {

  behavior of "InterpreterSpec"

  it should "apply" in {

    val runCache: UIO[Int] = for {
      counter <- Ref.make(0)
      _ <- counter.update(_ + 1).fork
      _ <- counter.update(_ + 1).fork
      r <- counter.get
    } yield r

    Interpreter.everyPath.terminatesAlwaysSuccessfully(runCache) should be(Set(0, 1, 2))
  }

}
