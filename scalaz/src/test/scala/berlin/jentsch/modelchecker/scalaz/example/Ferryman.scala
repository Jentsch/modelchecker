package berlin.jentsch.modelchecker.scalaz.example

import berlin.jentsch.modelchecker.scalaz.Interpreter.everyPath
import berlin.jentsch.modelchecker.scalaz.NonDeterministic
import berlin.jentsch.modelchecker.scalaz.NonDeterministic.doOneOf
import org.scalatest.matchers.Matcher
import org.scalatest.{Assertion, FlatSpec, Matchers}
import scalaz.zio.Exit.Success
import scalaz.zio.syntax._
import scalaz.zio.{Exit, _}

object Ferryman {

  type Error = Unit
  val Error: Error = ()

  def randomM: ZIO[NonDeterministic, Error, Unit] =
    for {
      wolf <- Ref.make(false)
      goat <- Ref.make(false)
      cabbage <- Ref.make(false)
      ferryMan <- Ref.make(false)
      limit <- Ref.make(8)
      _ <- repeatUntil(wolf.get && goat.get && cabbage.get && ferryMan.get)(
        doOneOf(
          sameSide(wolf, ferryMan) -> carry(wolf) *> carry(ferryMan),
          sameSide(goat, ferryMan) -> carry(goat) *> carry(ferryMan),
          sameSide(cabbage, ferryMan) -> carry(cabbage) *> carry(ferryMan),
          true.succeed -> carry(ferryMan)
        ) *>
          check(wolf.get =!= goat.get || wolf.get === ferryMan.get) *>
          check(goat.get =!= cabbage.get || goat.get === ferryMan.get) *>
          check(limit.update(_ - 1).map(_ > 0))
      )
    } yield ()

  /** Lifts some some boolean operations into UIO */
  implicit class UioBooleanSyntax(zio: UIO[Boolean]) {
    def &&(other: UIO[Boolean]): ZIO[Any, Nothing, Boolean] =
      zio.zipWith(other)(_ && _)

    def ||(other: UIO[Boolean]): ZIO[Any, Nothing, Boolean] =
      zio.zipWith(other)(_ || _)

    def ===(other: UIO[Boolean]): ZIO[Any, Nothing, Boolean] =
      zio.zipWith(other)(_ == _)

    def =!=(other: UIO[Boolean]): ZIO[Any, Nothing, Boolean] =
      zio.zipWith(other)(_ != _)
  }

  def carry(ref: Ref[Boolean]): ZIO[Any, Nothing, Unit] =
    ref.update(!_).void

  def sameSide(item1: Ref[Boolean], item2: Ref[Boolean]): UIO[Boolean] =
    item1.get.zipWith(item2.get)(_ == _)

  def repeatUntil[R, E](
      condition: UIO[Boolean]
  )(zio: ZIO[R, E, _]): ZIO[R, E, Unit] =
    condition.flatMap {
      case false => zio *> repeatUntil(condition)(zio)
      case true  => ZIO.unit
    }

  def check(predicate: UIO[Boolean]): IO[Error, Unit] =
    IO.whenM(predicate.map(!_))(ZIO.fail(Error))
}

class FerrymanSpec extends FlatSpec with Matchers {
  behavior of "ferryman"

  implicit class Syntax[E, A](results: Set[Exit[E, A]]) {
    def could(matcher: Matcher[Any]): Assertion =
      atLeast(1, results) should matcher
  }

  it should "be possible to carry over everything" in {
    everyPath.terminates(Ferryman.randomM) could be(a[Success[_]])
  }

}
