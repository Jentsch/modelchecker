package zio.modelchecker


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.{Ref, UIO}

class InterpreterSpec extends AnyFlatSpec with Matchers {

  behavior of "InterpreterSpec"

  it should "apply" in {

    val runCache: UIO[Int] = for {
      counter <- Ref.make(0)
      _ <- counter.update(_ + 1).fork
      _ <- counter.update(_ + 1).fork
      r <- counter.get
    } yield r

    Interpreter.everyPath.terminatesAlwaysSuccessfully(runCache) should be(
      Set(0, 1, 2)
    )
  }

}
