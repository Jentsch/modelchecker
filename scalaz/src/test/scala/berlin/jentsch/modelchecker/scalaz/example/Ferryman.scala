package berlin.jentsch.modelchecker.scalaz.example

import berlin.jentsch.modelchecker.scalaz.Interpreter.everyPath
import berlin.jentsch.modelchecker.scalaz.NonDeterministic
import berlin.jentsch.modelchecker.scalaz.NonDeterministic.doOneOf
import org.scalatest.matchers.Matcher
import org.scalatest.{Assertion, FlatSpec, Matchers}
import scalaz.zio.Exit.Success
import scalaz.zio._
import scalaz.zio.syntax._

object Ferryman {

  type Error = Unit
  val Error: Error = ()

  def ferryman: ZIO[NonDeterministic, Error, Unit] =
    for {
      wolf <- Ref.make(false)
      goat <- Ref.make(false)
      cabbage <- Ref.make(false)
      ferryMan <- Ref.make(false)
      limit <- Ref.make(8)
      _ <- repeatUntil(allCarriedOver(wolf, goat, cabbage, ferryMan))(
        doOneOf( // rules
          (wolf.get === ferryMan.get) -> carry(wolf) *> carry(ferryMan),
          (goat.get === ferryMan.get) -> carry(goat) *> carry(ferryMan),
          (cabbage.get === ferryMan.get) -> carry(cabbage) *> carry(ferryMan),
          true.succeed -> carry(ferryMan)
        ) *> // Check for invalid states
          check(wolf.get =!= goat.get || wolf.get === ferryMan.get) *>
          check(goat.get =!= cabbage.get || goat.get === ferryMan.get) *>
          check(limit.update(_ - 1).map(_ > 0))
      )
    } yield ()

  private def allCarriedOver(items: Ref[Boolean]*): UIO[Boolean] =
    items.foldLeft(true.succeed)((f, r) => f.zipWith(r.get)(_ && _))

  /** Lifts some some boolean operations into UIO */
  implicit class UioBooleanSyntax(zio: UIO[Boolean]) {
    def &&(other: UIO[Boolean]): UIO[Boolean] =
      zio.zipWith(other)(_ && _)

    def ||(other: UIO[Boolean]): UIO[Boolean] =
      zio.zipWith(other)(_ || _)

    def ===(other: UIO[Boolean]): UIO[Boolean] =
      zio.zipWith(other)(_ == _)

    def =!=(other: UIO[Boolean]): UIO[Boolean] =
      zio.zipWith(other)(_ != _)
  }

  def carry(ref: Ref[Boolean]): ZIO[Any, Nothing, Unit] =
    ref.update(!_).void

  def repeatUntil[R, E](
      condition: UIO[Boolean]
  )(zio: ZIO[R, E, _]): ZIO[R, E, Unit] =
    condition.flatMap {
      case false => zio *> repeatUntil(condition)(zio)
      case true  => ZIO.unit
    }

  def check(predicate: UIO[Boolean]): IO[Error, Unit] =
    IO.whenM(predicate.map(!_))(IO.fail(Error))
}

class FerrymanSpec extends FlatSpec with Matchers {
  behavior of "ferryman"

  implicit class Syntax[A](results: Set[A]) {
    def could(matcher: Matcher[A]): Assertion =
      atLeast(1, results) should matcher
  }

  it should "be possible to carry over everything" in {
    everyPath.terminates(Ferryman.ferryman) could be(a[Success[_]])
  }

}
