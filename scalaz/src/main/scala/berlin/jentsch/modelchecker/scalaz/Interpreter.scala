package berlin.jentsch.modelchecker.scalaz

import scalaz.zio.{Ref, ZIO}

object Interpreter {

  def concurrentEffectsCounter[R, E](zio: ZIO[R, E, _]): ZIO[R, E, Int] =
    concurrentEffectsCounterAndResult(zio).map(_._2)

  def concurrentEffectsCounterAndResult[R, E, A](
      zio: ZIO[R, E, A]): ZIO[R, E, (A, Int)] =
    for {
      counter <- Ref.make((1, 0))
      a <- rewrite(zio, counter)
      effects <- counter.get
    } yield (a, effects._2)

  def rewrite[R, E, A](io: ZIO[R, E, A],
                       counter: Ref[(Int, Int)]): ZIO[R, E, A] = {
    def rec[R2, E1, A1](io: ZIO[R2, E1, A1]): ZIO[R2, E1, A1] =
      rewrite(io, counter)

    io match {
      case value: ZIO.FlatMap[R, E, _, A] =>
        rec(value.zio).flatMap(x => rec(value.k(x)))
      case value: ZIO.Succeed[A] => value

      case value: ZIO.Effect[A] =>
        counter.update(incCounter) *> value
      case value: ZIO.EffectAsync[E, A] => counter.update(incCounter) *> value
      case value: ZIO.Fork[_, _] =>
        val prepIO =
          counter.update(incThreads) *>
            rec(value.value) ensuring
            counter.update(decThreads)

        prepIO.fork
      case value: ZIO.Uninterruptible[R, E, A] =>
        rec(value.zio).uninterruptible
      case value: ZIO.Supervise[R, E, A] =>
        rec(value.value).superviseWith(x => rec(value.supervisor(x)))
      case value: ZIO.Fail[E] => value
      case value: ZIO.Ensuring[R, E, A] =>
        rec(value.zio).ensuring(rec(value.finalizer))
      case d @ ZIO.Descriptor       => d
      case value: ZIO.Lock[R, E, A] => rec(value.zio).lock(value.executor)
      case y @ ZIO.Yield            => y
      case fold: ZIO.Fold[R, E, _, A, _] =>
        rec(fold.value)
          .foldCauseM(fold.err.andThen(rec), fold.succ.andThen(rec))
      case provide: ZIO.Provide[_, E, A] =>
        rec(provide.next).provide(provide.r)
      case read: ZIO.Read[R, E, A] =>
        ZIO.accessM(read.k.andThen(rec))
    }
  }

  private val incCounter: ((Int, Int)) => (Int, Int) = {
    case notConcurrent @ (1, _) => notConcurrent
    case (threads, effects)     => (threads, effects + 1)
  }

  private val incThreads: ((Int, Int)) => (Int, Int) = {
    case (threads, effects) => (threads + 1, effects)
  }

  private val decThreads: ((Int, Int)) => (Int, Int) = {
    case (threads, effects) => (threads - 1, effects)
  }

}
