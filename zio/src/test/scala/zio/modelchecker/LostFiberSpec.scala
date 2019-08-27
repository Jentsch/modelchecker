package zio.modelchecker

import org.scalatest.matchers.Matcher
import org.scalatest.{Assertion, FlatSpec, Matchers}

import zio._
import zio.Exit.Success
import zio.syntax._
import zio.modelchecker.Interpreter.terminatesAlwaysSuccessfully

class LostFiberSpec extends FlatSpec with Matchers {
  behavior of "RefM"

  val refM: UIO[Set[Int]] = for {
    invocated <- Ref.make(Set.empty[Int])
    incCount = (i: Int) => invocated.update(s => s + i).unit
    cache <- RefM.make(())
    invoke = (i: Int) => cache.update(_ => incCount(i))
    _ <- invoke(1).fork
    _ <- invoke(2).fork
    r <- invocated.get
  } yield r

  val results = terminatesAlwaysSuccessfully(refM)

  it can "be that we read before any invocation" in {
    results should contain(Set.empty)
  }

  // this test is really flaky
  ignore can "be that we read afterinvocation 1 and before 2" in {
    results should contain(Set(2))
  }

  // like the test above this test is also flaky
  ignore can "be that we read afterinvocation 2 and before 1" in {
    results should contain(Set(1))
  }

  // My hypnosis is that this test is also flaky but it's very unlikely that it
  // will ever succeed. It this is the case, than it shows the issues with a
  // random traverser
  it can "be that we read after both invocations" in {
    pendingUntilFixed {
      results should contain(Set(1, 2))
    }
  }
}
