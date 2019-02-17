package berlin.jentsch.modelchecker.scalaz

import scalaz.zio.Exit.Cause
import scalaz.zio.{IO, Ref}

object Interpreter {

  def effectCounter[E](io: IO[E, _]): IO[E, Int] =
    for {
      counter <- Ref.make(0)
      _ <- rewrite(io, counter)
      effects <- counter.get
    } yield effects

  def rewrite[E, A](io: IO[E, A], counter: Ref[Int]): IO[E, A] = {
    def rec[E1, A1](io: IO[E1, A1]): IO[E1, A1] =
      rewrite(io, counter)

    io match {
      case value: IO.FlatMap[E, _, A] =>
        rec(value.io).flatMap(x => rec(value.flatMapper(x)))
      case value: IO.Point[A]           => value
      case value: IO.Strict[A]          => value
      case value: IO.SyncEffect[A]      => counter.update(_ + 1) *> value
      case value: IO.AsyncEffect[E, A]  => counter.update(_ + 1) *> value
      case value: IO.Redeem[_, E, _, A] =>
        redeem(rec(value.value), value.err.andThen(rec), value.succ.andThen(rec))
      case value: IO.Fork[_, _] =>
        value.handler.fold(
          rec(value.value).fork
        )(
          rec(value.value).forkWith
        )
      case value: IO.Uninterruptible[E, A] =>
        rec(value.io).uninterruptible
      case value: IO.Supervise[E, A] =>
        rec(value.value).superviseWith(x => rec(value.supervisor(x)))
      case value: IO.Fail[_] => value
      case value: IO.Ensuring[E, A] =>
        rec(value.io).ensuring(rec(value.finalizer))
      case IO.Descriptor        => IO.Descriptor
      case value: IO.Lock[E, A] => rec(value.io).lock(value.executor)
      case IO.Yield             => IO.Yield
    }
  }

  private val redeemConstructor = classOf[IO.Redeem[_, _, _, _]].getConstructors.apply(0)
  redeemConstructor.setAccessible(true)

  private def redeem[E, E2, A, B](value: IO[E, A],
                                  err: Cause[E] => IO[E2, B],
                                  succ: A => IO[E2, B]): IO[E2, B] =
    redeemConstructor.newInstance(value, err, succ).asInstanceOf[IO.Redeem[E, E2, A, B]]

}
