package test

import scala.concurrent.ExecutionContext

object JpfExecutionContext extends ExecutionContext {
  override def execute(runnable: Runnable): Unit =
    new Thread(runnable).start()

  override def reportFailure(cause: Throwable): Unit =
    throw cause
}
