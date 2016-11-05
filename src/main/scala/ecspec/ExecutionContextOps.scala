package ecspec

import scala.concurrent.ExecutionContext

trait ExecutionContextOps {

  def atomic[T](block: => T)(implicit ec: ExecutionContext): T = {
    ec match {
      case tec: TestExecutionContext if !tec.atomic =>
        tec.atomic = true

        val result: T = block

        tec.atomic = false

        tec.pass()

        result
      case _ =>
        block
    }

  }

  def pass()(implicit ec: ExecutionContext): Unit = {
    ec match {
      case tec: TestExecutionContext =>
        tec.pass()
      case _ =>
    }

  }
}

/**
  * Import this object in your production code to user
  */
object ExecutionContextOps extends ExecutionContextOps
