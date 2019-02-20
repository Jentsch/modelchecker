package berlin.jentsch.modelchecker.futures

import scala.concurrent.ExecutionContext

trait ExecutionContextOps {

  /**
    * Within an atomic block the only when calling pass() explicitly a thread switch can happen.
    */
  def atomic[T](block: => T)(implicit ec: ExecutionContext): T = {
    val result = uninterrupted(block)
    pass()
    result
  }

  def uninterrupted[T](block: => T)(implicit ec: ExecutionContext): T =
    ec match {
      case tec: TestExecutionContext if !tec.atomic =>
        tec.atomic = true

        val result: T = block

        tec.atomic = false

        result
      case _ =>
        block
    }

  /**
    * Allows to interleave two atomic operations with a TestExecutionContext. Has no effects otherwise.
    *
    * @example of usage:
    * {{{
    * import scala.concurrent.Future
    * import java.util.concurrent.atomic.AtomicInteger
    * import berlin.jentsch.modelchecker.futures.EcSpec.everyInterleaving
    *
    * import berlin.jentsch.modelchecker.futures.ExecutionContextOps.pass
    *
    * everyInterleaving { implicit ec =>
    *   val result = new AtomicInteger(0)
    *   Future {
    *     result.set(1)
    *     result.set(0)
    *   }
    *
    *   result.get should not be 1 // which at runtime could happen
    * }
    *
    * everyInterleaving { implicit ec =>
    *   val result = new AtomicInteger(0)
    *   Future {
    *     result.set(1)
    *     pass
    *     result.set(0)
    *   }
    *
    *   result.get should (be(1) or be(0))
    * }
    *
    * }}}
    */
  def pass()(implicit ec: ExecutionContext): Unit =
    ec match {
      case tec: TestExecutionContext =>
        tec.pass()
      case _ =>
    }
}

/**
  * Import this object in your production code to use.
  */
object ExecutionContextOps extends ExecutionContextOps
