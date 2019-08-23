package zio.modelchecker.example

import org.scalatest.matchers.Matcher
import org.scalatest.{Assertion, FlatSpec, Matchers}

import zio._
import zio.Exit.Success
import zio.syntax._
import zio.modelchecker.Interpreter.everyPath
import zio.modelchecker.NonDeterministic
import zio.modelchecker.NonDeterministic.doOneOf

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

  def carry(ref: Ref[Boolean]): UIO[Unit] =
    ref.update(!_).unit

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
  behavior of "The Ferryman"

  implicit class Syntax[A](results: Set[A]) {
    def could(matcher: Matcher[A]): Assertion =
      atLeast(1, results) should matcher
  }

  it should "be able to carry over everything" in {
    everyPath.notFailing(Ferryman.ferryman.run) could be(Some(Success(())))
  }

}
