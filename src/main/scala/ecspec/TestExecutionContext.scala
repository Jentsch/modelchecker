package ecspec

import java.util.concurrent.{Semaphore, TimeUnit}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
  * Fake execution context for EcSpec.
  *
  * Invariant: either two threads a running an within this class active (handling semaphores) or one threads
  * runs provided test code.
  *
  * Every interaction between threads is modelled with semaphores to ensure the synchronisation of variables by the JVM (happens before).
  * See [[https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Semaphore.html Semaphore-API]]
  */
class TestExecutionContext extends ExecutionContext { self =>

  private[this] val waitingList = mutable.Buffer[Semaphore]()
  private[this] val finalStop = new Semaphore(0)
  private[this] val traverser = new Traverser

  /**
    * If atomic is set no thread switch happens.
    */
  private[ecspec] var atomic = false
  private[ecspec] var foundException = Option.empty[Throwable]

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

  /**
    * Allows to pass the control explicitly to an other random thread.
    *
    * With pass:
    * {{{
    * var observedInterleaving = false
    * val testEC = new TestExecutionContext
    * import testEC._
    *
    * testEveryPath{ implicit ec =>
    *   var x = true
    *
    *   ec.execute { () =>
    *     x = false
    *     pass()
    *     // The other thread below can now interleave this thread
    *     // and set x to true
    *     observedInterleaving |= x
    *   }
    *
    *   ec.execute { () =>
    *     x = true
    *   }
    * }
    *
    * observedInterleaving should be(true)
    * }}}
    *
    * In comparison without pass:
    * {{{
    * var observedInterleaving = false
    * val testEC = new TestExecutionContext
    * import testEC._
    *
    * testEveryPath{ implicit ec =>
    *   var x = true
    *
    *   ec.execute { () =>
    *     x = false
    *     // no interleaving possible x is always false
    *     observedInterleaving |= x
    *   }
    *
    *   ec.execute { () =>
    *     x = true
    *   }
    * }
    *
    * observedInterleaving should be(false)
    * }}}
    *
    */
  def pass(): Unit = {
    val ownSemaphore = new Semaphore(0)
    waitingList += ownSemaphore
    chooseNextThread()
    ownSemaphore.acquire()
  }

  /**
    * Creates a thread that is locked by a semaphore. The caller can start the thread by releasing the
    * semaphore once. After that the semaphore as no more meaning.
    *
    * {{{
    *   val tec = TestExecutionContext()
    *
    *   val run = new java.util.concurrent.atomic.AtomicInteger(0)
    *   val sem = tec.createStoppedThread{ () => run.set(1) }
    *   Thread.sleep(100)
    *
    *   run.get should be(0)
    *
    *   sem.release
    *   Thread.sleep(100)
    *
    *   run.get should be(1)
    * }}}
    */
  private[ecspec] def createStoppedThread(runnable: Runnable): Semaphore = {
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

object TestExecutionContext {
  def apply(): TestExecutionContext = new TestExecutionContext
}
