package berlin.jentsch.modelchecker.futures

import java.util.concurrent._

import berlin.jentsch.modelchecker.{
  EveryPathTraverser,
  RandomTraverser,
  SinglePath,
  Traverser
}
import org.scalatest.exceptions.TestFailedException

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
  * Fake execution context for [[EcSpec]].
  */
/* Invariant: either two threads a running an within this class active (handling semaphores) or only one
 * threads runs provided test code.
 *
 * Every interaction between threads is modelled with semaphores to ensure the synchronisation of variables by the JVM (happens before).
 * See [[https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Semaphore.html Semaphore-API]]
 */
class TestExecutionContext(info: String => Unit,
                           private[this] val traverser: Traverser)
    extends ExecutionContext {

  /** All waiting threads, all semaphores have -1 permits */
  private[this] val waitingList = mutable.Buffer[Semaphore]()
  private[this] val finalStop = new Semaphore(0)
  private[this] val finalChecks = mutable.Buffer.empty[() => Unit]
  private[this] val executor = TestExecutionContext.globalThreadPool

  /**
    * If atomic is set no thread switch happens.
    */
  private[futures] var atomic = false
  private[futures] var foundException = Option.empty[Throwable]

  def testEveryPath(test: TestExecutionContext => Unit): Unit = {
    var finalStates = 0
    do {
      test(this)
      runNextThread()

      val maxSeconds = 60L

      val noOpenThreads = finalStop.tryAcquire(maxSeconds, TimeUnit.SECONDS)

      if (!noOpenThreads && waitingList.nonEmpty) {
        runNextThread()

        val noOpenThreads2 = finalStop.tryAcquire(maxSeconds, TimeUnit.SECONDS)
        assert(
          noOpenThreads2,
          s"Couldn't finish the test within $maxSeconds second. Open ${waitingList.size} Threads. Discovered $finalStates final states."
        )
      }

      assert(waitingList.isEmpty)

      finalStates += 1
      if (foundException.nonEmpty)
        info("Bal")
      foundException.foreach(throw _)
      finalChecks.foreach(check =>
        try { check() } catch {
          case failed: TestFailedException =>
            info("Tested paths: " ++ finalStates.toString)
            info(
              "Path to reproduce this failure: " ++ traverser.getCurrentPath
                .mkString("Seq(", ",", ")"))
            throw failed
      })
      finalChecks.clear()
    } while (traverser.hasMoreOptions())

    info("Paths: " ++ finalStates.toString)
  }

  override def execute(runnable: Runnable): Unit = {
    waitingList += createStoppedThread(runnable)
    if (!atomic)
      pass()
  }

  /**
    * Allows to pass the control explicitly to an other random thread.
    *
    * @example With pass:
    * {{{
    * var observedInterleaving = false
    * val testEC = TestExecutionContext()
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
    * @example In comparison without pass:
    * {{{
    * var observedInterleaving = false
    * val testEC = TestExecutionContext()
    * import testEC._
    *
    * testEveryPath { implicit ec =>
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
    if (waitingList.nonEmpty) {
      val ownSemaphore = new Semaphore(0)
      waitingList += ownSemaphore
      runNextThread()
      ownSemaphore.acquire()
    }
  }

  /**
    * Creates a thread that is locked by a semaphore. The caller can start the thread by releasing the
    * semaphore once. After that the semaphore has no more meaning.
    *
    * @example
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
  private[futures] def createStoppedThread(runnable: Runnable): Semaphore = {
    val startSignal = new Semaphore(0)

    executor.execute { () =>
      startSignal.acquire()
      try {
        runnable.run()
      } catch {
        case NonFatal(thrown) =>
          foundException = Some(thrown)
      } finally {
        runNextThread()
      }
    }

    startSignal
  }

  private def runNextThread(): Unit =
    if (waitingList.isEmpty) {
      finalStop.release()
    } else {
      val nextThread = traverser.removeOne(waitingList)
      nextThread.release()
    }

  override def reportFailure(cause: Throwable): Unit =
    foundException = Some(cause)

  private[futures] def finallyCheck(check: () => Unit): Unit =
    finalChecks += check
}

object TestExecutionContext {

  /**
    * Creates a TestExecutionContext which prints to stdout and stderr.
    */
  def apply(): TestExecutionContext =
    new TestExecutionContext(println, new EveryPathTraverser)

  def testEveryPath(test: TestExecutionContext => Unit,
                    info: String => Unit): Unit =
    new TestExecutionContext(info, new EveryPathTraverser).testEveryPath(test)

  def testSinglePath(test: TestExecutionContext => Unit,
                     path: Seq[Int],
                     info: String => Unit): Unit =
    new TestExecutionContext(info, new SinglePath(path)).testEveryPath(test)

  def testRandomPath(test: TestExecutionContext => Unit,
                     info: String => Unit): Unit =
    new TestExecutionContext(info, new RandomTraverser(100)).testEveryPath(test)

  private[TestExecutionContext] val globalThreadPool =
    new ThreadPoolExecutor(0,
                           Integer.MAX_VALUE,
                           10,
                           TimeUnit.SECONDS,
                           new SynchronousQueue[Runnable])
}
