package ecspec

import java.util.concurrent.{Semaphore, TimeUnit}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
  * Fake execution context for EcSpec.
  */
private[ecspec] class TestExecutionContext extends ExecutionContext { self =>

  private val waitingList = mutable.Buffer[Semaphore]()
  private val finalStop = new Semaphore(0)
  private val traverser = new Traverser

  /**
    * If atomic is set no thread switch happens
    */
  var atomic = false
  var foundException = Option.empty[Throwable]

  def testEveryPath(test: (ExecutionContext) => Unit): Unit = {
    var stateSpaceSize = 0
    do {
      test(self)
      chooseNextThread()

      val noOpenThreads = finalStop.tryAcquire(10, TimeUnit.SECONDS)
      assert(
        noOpenThreads,
        s"Couldn't finish the test within 1 second. Open ${waitingList.size} Threads.")
      assert(waitingList.isEmpty)

      stateSpaceSize += 1
      foundException.foreach(throw _)
    } while (traverser.hasMoreOptions)

    println("State space size: " + stateSpaceSize)
  }

  override def execute(runnable: Runnable): Unit = {
    waitingList += createStoppedThread(runnable)
    if (!atomic)
      pass()
  }

  /** Allows to pass the control to an other thread */
  def pass(): Unit = {
    val ownSemaphore = new Semaphore(0)
    waitingList += ownSemaphore
    chooseNextThread()
    ownSemaphore.acquire()
  }

  private def createStoppedThread(runnable: Runnable): (Semaphore) = {
    val startSignal = new Semaphore(0)

    val thread: Thread = new Thread {
      override def run(): Unit = {
        startSignal.acquire()
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

    startSignal
  }

  private def chooseNextThread() = {
    if (waitingList.isEmpty) {
      finalStop.release()
    } else {
      traverser.removeOne(waitingList).release()
    }
  }

  override def reportFailure(cause: Throwable): Unit = throw cause
}
