package zio.modelchecker

import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.{AssertResult, FailureDetails, TestResult, ZSpec, assert, testM}

object NonDeterministicSpec {
  def testAlways[L](label: L)(
      nonDeterministic: ZIO[NonDeterministic, Nothing, TestResult]
  ): ZSpec[Any, Nothing, L, Unit] = {

    val results =
      Interpreter.everyPath.terminatesAlwaysSuccessfully(nonDeterministic)
    val ok: AssertResult[Either[FailureDetails, Unit]] =
      AssertResult.success(())
    val assertions = results.filter(_.isFailure).take(10).foldLeft(ok)(_ && _)

    testM(label)(ZIO.succeed(assertions))
  }

  def testSometimes[L](label: L)(
      nonDeterministic: ZIO[NonDeterministic, Nothing, TestResult]
  ): ZSpec[Any, Nothing, L, Unit] = {

    val results =
      Interpreter.everyPath.terminatesAlwaysSuccessfully(nonDeterministic)

    val message =
      assert(results.count(_.isSuccess) >= 1, equalTo(true))

    testM(label)(ZIO.succeed(message))
  }
}
