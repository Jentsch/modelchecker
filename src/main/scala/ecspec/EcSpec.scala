package ecspec

import org.scalactic.source.Position
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.{MatchFailed, Matcher}
import org.scalatest.{Informer, Matchers}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.reflect.macros.blackbox
import scala.util.{Failure, Success}

/**
  * Add this trait to your test class to use the [[#everyInterleaving]] method.
  *
  * Typical usage should look like:
  * {{{
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
  *       x could be(1) // in case of an interleaving x can be actual 1
  *       x could be(2) // No interleaving
  *     }
  *   }
  * }}}
  *
  * @see #everyInterleaving
  */
trait EcSpec extends ExecutionContextOps { self: Matchers =>

  /** Don't implement this method, but mixin the EcSpec into a FlatSpec, WordSpec etc. */
  protected def info: Informer

  implicit val ecContext: EcSpec = this

  private val couldWasTrueFor =
    mutable.Map.empty[String, Option[TestFailedException]]

  /**
    * It's possible that not every computation is done after returning to the test.
    *
    * The tests written with this method can detect race conditions if all side effects of a single action happens in
    * an atomic way and that semantic hold even with the jvm memory model.
    *
    * Ok:
    * {{{
    * import scala.concurrent.Future
    * import ecspec.EcSpec._
    *
    * everyInterleaving { implicit ec =>
    *   @volatile var x = 1
    *
    *   Future { x = 2 }
    *   Future { x = 3 }
    * }
    * }}}
    * Without the volatile annotation tests would be ok but the runtime semantic isn't covered by the tests.
    *
    * @param test the test code to rum
    */
  def everyInterleaving(test: TestExecutionContext => Unit)(
      implicit pos: Position): Unit = {
    TestExecutionContext(info.apply(_, None)).testEveryPath(test)

    throwExceptionForNeverSatisfiedCouldTests()
  }

  private def throwExceptionForNeverSatisfiedCouldTests(): Unit =
    couldWasTrueFor.values.flatten.headOption
      .foreach {
        throw _
      }

  /**
    * Checks if a value only increase over time.
    *
    * {{{
    * import scala.concurrent.Future
    * import java.util.concurrent.atomic.AtomicInteger
    * import ecspec.EcSpec._
    *
    * everyInterleaving { implicit ec =>
    *   val x = new AtomicInteger(0)
    *
    *   Future { x.incrementAndGet }
    *   Future { x.incrementAndGet }
    *   // Future { x.decrementAndGet } would break this test
    *
    *   x.get should increase
    * }
    *
    * }}}
    */
  def increase[T: Ordering]: TimeWord[T] = new TimeWord[T] {
    override def apply(t: => T)(implicit ec: ExecutionContext): Unit = {
      Future {
        t
      }.foreach { t1 =>
        val t2 = t

        t1 should be <= t2
      }
    }
  }

  implicit def toCouldTestWord[T](value: T): EcSpec.CouldTestWord[T] =
    macro EcSpec.ToCouldTestWordImpl[T]

  /**
    * Properties about a term `t` and it's behaviour over time.
    */
  trait TimeWord[T] {
    def apply(t: => T)(implicit ec: ExecutionContext): Unit
  }

  /**
    * Add a should that accepts [[TimeWord]], properties about state over time.
    *
    * @see ecspec.EcSpec#increase
    */
  implicit class TimeWordShould[T](t: => T) {
    def should(timeWord: TimeWord[T])(implicit ec: ExecutionContext): Unit = {
      timeWord(t)
    }
  }

  /**
    * {{{
    * import scala.concurrent.Future
    * import java.util.concurrent.atomic.AtomicInteger
    * import ecspec.EcSpec.everyInterleaving
    * import ecspec.EcSpec.WillWord
    *
    * everyInterleaving { implicit ec =>
    *   val x = Future { 1 }
    *   val y = Future { 2 }
    *
    *   x will be(1)
    *
    *   Future.firstCompletedOf(x :: y :: Nil) will (be(1) or be(2))
    * }
    *
    * }}}
    */
  implicit class WillWord[T](t: Future[T]) {
    def will(matcher: Matcher[T])(implicit ec: TestExecutionContext): Unit =
      ec.hookAfterStep { () =>
        t.value match {
          case Some(Success(s)) =>
            s should matcher
            false
          case Some(Failure(f)) =>
            throw f
          case None => true
        }
      }
  }

}

/**
  * Instead of mixin the EcSpec trait you can also do a wildcard import of this object.
  */
object EcSpec extends Matchers with EcSpec {
  def ToCouldTestWordImpl[T: c.WeakTypeTag](c: blackbox.Context)(
      value: c.Expr[T]): c.Expr[CouldTestWord[T]] = {
    import c.universe._

    val position = c.enclosingPosition
    val pos = position.source.path + " " + position.line.toString + ":" + position.column.toString
    val fileContent = new String(value.tree.pos.source.content)
    val start = value.tree.pos.start
    val txt = fileContent.slice(start, start + 1)

    val tree =
      q"""new ecspec.EcSpec.CouldTestWord[${weakTypeOf[T]}]($value, $txt, $pos)"""

    c.Expr[CouldTestWord[T]](tree)
  }

  class CouldTestWord[T](value: T, valRep: String, pos: String) {

    /**
      * This method enables syntax such as the following:
      *
      * ```
      * result could be (3)
      * ```
      **/
    def could(rightMatcher: Matcher[T])(implicit ctx: EcSpec): Unit = {
      rightMatcher(value) match {
        case MatchFailed(_) =>
          ctx.couldWasTrueFor.getOrElseUpdate(
            pos,
            Some(
              new TestFailedException(
                valRep + " couldn't " + rightMatcher.toString(),
                13)))
          ()
        case _ =>
          ctx.couldWasTrueFor(pos) = None

          ()
      }
    }

  }

  override protected def info: Informer = new Informer {
    override def apply(message: String, payload: Option[Any])(
        implicit pos: Position): Unit =
      println(s"$message: $payload at $pos")
  }
}
