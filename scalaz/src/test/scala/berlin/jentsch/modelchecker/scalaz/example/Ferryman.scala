package berlin.jentsch.modelchecker.scalaz.example

import berlin.jentsch.modelchecker.scalaz.Interpreter
import org.scalatest.{FlatSpec, Matchers}
import scalaz.zio._
import scalaz.zio.random.Random
import scalaz.zio.syntax._

object Ferryman {

  type Error = Unit

  implicit class RefSyntax(zio: UIO[Boolean]) {
    def &&(other: UIO[Boolean]): ZIO[Any, Nothing, Boolean] =
      zio.zipWith(other)(_ && _)

    def ||(other: UIO[Boolean]): ZIO[Any, Nothing, Boolean] =
      zio.zipWith(other)(_ || _)

    def ===(other: UIO[Boolean]): ZIO[Any, Nothing, Boolean] =
      zio.zipWith(other)(_ == _)

    def =!=(other: UIO[Boolean]): ZIO[Any, Nothing, Boolean] =
      zio.zipWith(other)(_ != _)
  }

  def randomM: ZIO[Random, Error, Unit] =
    for {
      wolf <- Ref.make(false)
      goat <- Ref.make(false)
      cabbage <- Ref.make(false)
      ferryMan <- Ref.make(false)
      _ <- repeatUntil(wolf.get && goat.get && cabbage.get && ferryMan.get) {
        for {
          _ <- doOneOf(
            (wolf.get === ferryMan.get) -> wolf.update(!_) *> ferryMan.update(!_),
            (goat.get === ferryMan.get) -> goat.update(!_) *> ferryMan.update(!_),
            (cabbage.get === ferryMan.get) -> cabbage.update(!_) *> ferryMan.update(!_),
            true.succeed -> ferryMan.update(!_)
          )
          _ <- check(wolf.get =!= goat.get || wolf.get === ferryMan.get)
          _ <- check(goat.get =!= cabbage.get || goat.get === ferryMan.get)
        } yield ()
      }
    } yield ()

  def repeatUntil[R, E, A](condition: UIO[Boolean])(
      zio: ZIO[R, E, _]): ZIO[R, E, Unit] =
    condition.flatMap {
      case false =>  zio *> repeatUntil(condition)(zio)
      case true => ZIO.unit
    }

  def check(predicate: UIO[Boolean]): IO[Error, Unit] =
    predicate.flatMap {
      case true => ZIO.unit
      case false => ZIO.fail(())
    }


  def doOneOf[E, A](
      actions: (UIO[Boolean], ZIO[Any, E, _])*): ZIO[Random, E, Unit] = {
    val options: UIO[Seq[ZIO[Any, E, Unit]]] =
      actions.foldLeft(ZIO.succeed(Seq.empty[ZIO[Any, E, Unit]])) {
        case (found, (predicate, action)) =>
          predicate.flatMap {
            case true  => found.map(_ :+ action.void)
            case false => found
          }
      }

    for {
      o <- options
      r <- random.nextInt(o.size)
      s <- o(r)
    } yield s
  }

}

class FerrymanSpec extends FlatSpec with Matchers {
  behavior of "ferryman"

  it should "be possible to carry over everything" in {
    Interpreter(Ferryman.randomM) should contain(Some(Exit.succeed(())))
  }
}
