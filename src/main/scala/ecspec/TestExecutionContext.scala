package ecspec

import java.util.concurrent.Semaphore

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
  * Fake execution context for EcSpec.
  */
private[ecspec] class TestExecutionContext extends ExecutionContext {
  self =>
  val waitingList: mutable.Buffer[Semaphore] = mutable.Buffer.empty[Semaphore]
  val finalStop = new Semaphore(0)
  private val traverser = new Traverser
  var foundException = Option.empty[Throwable]

  def testEveryPath(test: (ExecutionContext) => Unit) = {
    do {
      test(self)

      foundException.foreach(throw _)
    } while (traverser.hasMoreOptions)
  }

  override def execute(runnable: Runnable): Unit = {
    waitingList += createStoppedThread(runnable)
    val ownSemaphore = new Semaphore(0)
    waitingList += ownSemaphore
    chooseNextThread()
    ownSemaphore.acquire()
  }

  private def createStoppedThread(runnable: Runnable): (Semaphore) = {
    val stopSignal = new Semaphore(0)

    val thread: Thread = new Thread {
      override def run(): Unit = {
        stopSignal.acquire()
        try {
          runnable.run()
        } catch {
          case NonFatal(thrown) =>
            foundException = Some(thrown)
        }
        chooseNextThread()
      }
    }
    thread.start()

    stopSignal
  }

  private def chooseNextThread() = {
    if (waitingList.isEmpty) {
      finalStop.release()
    } else {
      val chosen = traverser.choose(waitingList)
      waitingList -= chosen
      chosen.release()
    }
  }

  override def reportFailure(cause: Throwable): Unit = throw cause
}
