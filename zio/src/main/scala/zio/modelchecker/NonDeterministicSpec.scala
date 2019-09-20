package zio.modelchecker

import zio.ZIO
import zio.test.{AssertResult, FailureDetails, TestResult, ZSpec, testM}

object NonDeterministicSpec {
  def testD[L](label: L)(
      nonDeterministic: ZIO[NonDeterministic, Nothing, TestResult]
  ): ZSpec[Any, Nothing, L, Unit] = {

    val results =
      Interpreter.everyPath.terminatesAlwaysSuccessfully(nonDeterministic)
    val ok: AssertResult[Either[FailureDetails, Unit]] =
      AssertResult.success(())
    val assertions = results.filter(_.isFailure).take(10).foldLeft(ok)(_ && _)
    testM(label)(ZIO.succeed(assertions))
  }
}
