package actors.modelchecking

import java.util.concurrent.Semaphore

import org.scalatest.Matchers
import org.scalatest.matchers.Matcher

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext

trait EcSpec extends Matchers {

  private var couldWasTrueFor = TrieMap.empty[sourcecode.Line, Boolean]

  /**
    *
    * ```
    * "test" in everyPossiblePath { implicit ec =>
    * ...TestCode...
    * }
    * ```
    *
    * It's possible that not every computation is done after returning to the test
    *
    * @param test the test code to rum
    */
  def everyPossiblePath(test: ExecutionContext => Unit) = {
    for (_ <- 1 to 100) {
      val ec = new TestExecutionContext
      ec.run(test)
    }
    couldWasTrueFor.foreach {
      case (pos, false) =>
        println("Line: " + pos)
        assert(false)
      case _ =>
    }
  }

  implicit class TestWords[T](value: T)(implicit pos: sourcecode.Line) {
    /**
      * This method enables syntax such as the following:
      *
      * ```
      * result could be (3)
      * ```
      **/
    def could(rightMatcherX1: Matcher[T]) {
      couldWasTrueFor(pos) = couldWasTrueFor.getOrElse(pos, false) | rightMatcherX1(value).matches
    }

  }

  private class TestExecutionContext extends ExecutionContext {
    self =>
    val waitingList = TrieMap.empty[String, Semaphore]
    val finalStop = new Semaphore(0)

    def run(test: ExecutionContext => Unit): Unit = {
      test(self)
    }

    override def execute(runnable: Runnable): Unit = {
      waitingList += createStoppedThread(runnable)
      val ownSemaphore = new Semaphore(0)
      waitingList += (Thread.currentThread().getName -> ownSemaphore)
      chooseNextThread()
      ownSemaphore.acquire()
    }

    def createStoppedThread(runnable: Runnable): (String, Semaphore) = {
      val stopSignal = new Semaphore(0)

      val thread: Thread {def run(): Unit} = new Thread {
        override def run(): Unit = {
          stopSignal.acquire()
          runnable.run()
          chooseNextThread()
        }
      }
      thread.start()

      thread.getName -> stopSignal
    }

    def chooseNextThread() = {
      if (waitingList.isEmpty) {
        finalStop.release()
      } else {
        val name: String = waitingList.keys.toList.apply(scala.util.Random.nextInt(waitingList.size))
        val Some(chosen) = waitingList.remove(name)
        chosen.release()
      }
    }

    override def reportFailure(cause: Throwable): Unit = throw cause
  }


}
