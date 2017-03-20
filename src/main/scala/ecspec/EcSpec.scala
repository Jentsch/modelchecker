package ecspec

import org.scalatest.Matchers
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.{MatchFailed, Matcher}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.reflect.macros.blackbox

/**
  * Add this trait to your test class to use the `everyPossiblePath` method.
  *
  * @see #everyPossiblePath
  */
trait EcSpec extends Matchers with ExecutionContextOps {

  protected implicit val ecSpecSelf: EcSpec = this

  private val couldWasTrueFor =
    mutable.Map.empty[Int, Option[TestFailedException]]

  /**
    *
    * ```
    * "test" in everyPossiblePath { implicit ec =>
    * ...TestCode...
    * }
    * ```
    *
    * It's possible that not every computation is done after returning to the test.
    *
    * The tests written with this method can detect race conditions if all side effects of a single action happens in
    * an atomic way and that semantic hold even with the jvm memory model.
    *
    * Ok:
    * {{{
    *   @volatile var x = 1
    *
    *   Future { x = 2 }
    *   Future { x = 3 }
    * }}}
    * Without the volatile annotation tests would be ok but the runtime semantic isn't covered by the tests.
    *
    * @param test the test code to rum
    */
  def everyPossiblePath(test: ExecutionContext => Unit) = {
    val ec = new TestExecutionContext
    ec.testEveryPath(test)

    couldWasTrueFor.foreach {
      case (pos, Some(matchResult)) =>
        throw matchResult

      case _ =>
    }
  }

  /**
    * Checks if a value only increase over time.
    *
    * {{{
    * import java.util.concurrent.atomic.AtomicInteger
    * val x = new AtomicInteger(0)
    *
    * Future { x.incrementAndGet }
    * Future { x.incrementAndGet }
    * // Future { x.decrementAndGet }
    *
    * x.value should increase
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

  implicit class TimeWordShould[T](t: => T) {
    def should(timeWord: TimeWord[T])(implicit ec: ExecutionContext): Unit = {
      timeWord(t)
    }
  }

}

object EcSpec {
  def ToCouldTestWordImpl[T: c.WeakTypeTag](c: blackbox.Context)(
      value: c.Expr[T]): c.Expr[CouldTestWord[T]] = {
    import c.universe._

    val line = c.enclosingPosition.line
    val fileContent = new String(value.tree.pos.source.content)
    val start = value.tree.pos.start
    val txt = fileContent.slice(start, start + 1)

    val tree =
      q"""new ecspec.EcSpec.CouldTestWord[Int]($value, $txt, $line)"""

    c.Expr[CouldTestWord[T]](tree)
  }

  class CouldTestWord[T](value: T, valRep: String, pos: Int) {

    /**
      * This method enables syntax such as the following:
      *
      * ```
      * result could be (3)
      * ```
      **/
    def could(rightMatcher: Matcher[T])(implicit ctx: EcSpec): Unit = {
      rightMatcher(value) match {
        case MatchFailed(failureMessage) =>
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

}