package berlin.jentsch.modelchecker.futures

import org.scalactic.source.Position
import org.scalatest.exceptions.{StackDepthException, TestFailedException}
import org.scalatest.matchers.{MatchFailed, Matcher}
import org.scalatest.{Informer, Matchers}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Add this trait to your test class to use the [[EcSpec.everyInterleaving()]] method.
  *
  * @example Typical usage should look like:
  *
  * {{{
  *   import org.scalatest.FlatSpec
  *   import org.scalatest.Matchers
  *
  *   class MyObjectSpec extends FlatSpec with Matchers with EcSpec {
  *     "Increment" should "not be an atomic action" in everyInterleaving { implicit ec =>
  *       // the main code is executed by thread 0
  *       @volatile var x = 0
  *
  *       ec.execute { () => // thread 1
  *         val t = x
  *         pass()
  *         x = t + 1
  *       }
  *
  *       ec.execute { () => // thread 2
  *         val t = x
  *         pass()
  *         x = t + 1
  *       }
  *
  *       x should be >= 0 // Invariant of this code x is 0, 1, or 2
  *       x should be <= 2
  *
  *       x could be(0) // Thread 0 can be done before Thread 1 or 2 even start
  *       x could be(1) // in case of an interleaving x can be 1
  *       x could be(2) // No interleaving and threads 1 and 2 finish before this 'main' thread
  *     }
  *   }
  * }}}
  * @see #everyInterleaving
  */
trait EcSpec extends ExecutionContextOps { self: Matchers =>

  /** Don't implement this method, but mixin the EcSpec into a FlatSpec, WordSpec etc. */
  protected def info: Informer

  private val couldWasTrueFor =
    mutable.Map.empty[Position, Option[TestFailedException]]

  /**
    * It's possible that not every computation is done after returning to the test.
    *
    * The tests written with this method can detect race conditions if all side effects of a single action happens in
    * an atomic way and that semantic hold even with the jvm memory model.
    *
    * @example Ok:
    * {{{
    * import scala.concurrent.Future
    * import berlin.jentsch.modelchecker.futures.EcSpec._
    *
    * everyInterleaving { implicit ec =>
    *   @volatile var x = 1
    *
    *   Future { x = 2 }
    *   Future { x = 3 }
    * }
    * }}}
    *
    * Without the volatile annotation tests would be still ok but the runtime semantic isn't covered by the tests.
    * @param test the test code to run
    */
  def everyInterleaving(test: TestExecutionContext => Unit): Unit = {
    TestExecutionContext.testEveryPath(test, info(_))

    throwExceptionForNeverSatisfiedCouldTests()
  }

  /**
    * This helper method runs only a single path.
    * All could assumptions will be ignored!
    *
    * @param path concrete path provided by the other everyInterleaving method
    * @param test the test code to run
    */
  def everyInterleaving(
      path: Seq[Int]
  )(test: TestExecutionContext => Unit): Unit =
    TestExecutionContext.testSinglePath(test, path, info(_))

  def randomInterleaving(test: TestExecutionContext => Unit): Unit =
    TestExecutionContext.testRandomPath(test, info(_))

  private def throwExceptionForNeverSatisfiedCouldTests(): Unit =
    couldWasTrueFor.values.flatten.headOption
      .foreach {
        throw _
      }

  /**
    * Checks if a value only increase over time.
    *
    * @example
    * {{{
    * import scala.concurrent.Future
    * import java.util.concurrent.atomic.AtomicInteger
    * import berlin.jentsch.modelchecker.futures.EcSpec._
    *
    * everyInterleaving { implicit ec =>
    *   val x = new AtomicInteger(0)
    *
    *   Future { x.incrementAndGet }
    *   Future { x.incrementAndGet }
    *   // Future { x.decrementAndGet } would break this test
    *
    *   x.get shouldOnly increase
    * }
    * }}}
    */
  def increase[T: Ordering]: TimeWord[T] = new TimeWord[T] {
    override def apply(
        t: => T
    )(implicit ec: ExecutionContext, position: Position): Unit = {
      Future {
        t
      }.foreach { t1 =>
        val t2 = t

        t1 should be <= t2
      }
    }
  }

  implicit class CouldTestWord[T](value: T) {

    /**
      * This method enables syntax such as the following:
      *
      * ```
      * result could be (3)
      * ```
      **/
    def could(rightMatcher: Matcher[T])(implicit position: Position): Unit = {
      rightMatcher(value) match {
        case MatchFailed(_) =>
          self.couldWasTrueFor.getOrElseUpdate(
            position,
            Some(
              new TestFailedException(
                (_: StackDepthException) =>
                  Some(" couldn't " + rightMatcher.toString()),
                None,
                Left(position),
                None
              )
            )
          )

          ()
        case _ =>
          self.couldWasTrueFor(position) = None

          ()
      }
    }

  }

  /**
    * Properties about a term `t` and it's behaviour over time.
    */
  trait TimeWord[T] {
    def apply(t: => T)(implicit ec: ExecutionContext, position: Position): Unit
  }

  /**
    * Add a should that accepts [[TimeWord]], properties about state over time.
    *
    * @see [[berlin.jentsch.modelchecker.futures.EcSpec.increase]]
    */
  implicit class TimeWordShould[T](t: => T) {
    def shouldOnly(timeWord: TimeWord[T])(implicit ec: ExecutionContext): Unit = {
      timeWord(t)
    }
  }

  /**
    * Waits until a future is completed and applies that the given matcher.
    * Doesn't fail if the future never completes.
    *
    * @example usage
    * {{{
    * import scala.concurrent.Future
    * import berlin.jentsch.modelchecker.futures.EcSpec.everyInterleaving
    * import berlin.jentsch.modelchecker.futures.EcSpec.WillWord
    *
    * everyInterleaving { implicit ec =>
    *   val x = Future { 1 }
    *
    *   x will be(1)
    * }
    * }}}
    */
  implicit class WillWord[T](t: Future[T]) {
    def will(
        matcher: Matcher[T]
    )(implicit ec: TestExecutionContext, position: Position): Unit =
      ec.finallyCheck { () =>
        import org.scalatest.TryValues._

        t.value match {
          case Some(t) =>
            t.success.value should matcher
          case None =>
            fail("All computations are done, but the Future wasn't completed")
        }
      }

    def will(
        complete: self.complete.type
    )(implicit ec: TestExecutionContext, position: Position): Unit =
      ec.finallyCheck { () =>
        if (!t.isCompleted)
          fail("All computations are done, but the Future wasn't completed")
      }

    def willNot(
        complete: self.complete.type
    )(implicit ec: TestExecutionContext, position: Position): Unit =
      ec.finallyCheck { () =>
        if (t.isCompleted)
          fail("The future was completed with " ++ t.value.get.toString)
      }
  }

  object complete
}

/**
  * Instead of mixin the EcSpec trait you can also do a wildcard import of this companion object.
  */
object EcSpec extends Matchers with EcSpec {
  override protected def info: Informer = new Informer {
    override def apply(message: String, payload: Option[Any])(
        implicit pos: Position
    ): Unit =
      println(s"$message: $payload at $pos")
  }
}
