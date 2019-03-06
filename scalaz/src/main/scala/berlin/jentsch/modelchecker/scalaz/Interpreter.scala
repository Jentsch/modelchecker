package berlin.jentsch.modelchecker.scalaz

import scalaz.zio._

object Interpreter {
  def concurrentEffectsCounter[R, E](zio: ZIO[R, E, _]): ZIO[R, E, Int] =
    concurrentEffectsCounterAndResult(zio).map(_._2)

  def concurrentEffectsCounterAndResult[R, E, A](
      zio: ZIO[R, E, A]): ZIO[R, E, (A, Int)] =
    for {
      counter <- Ref.make((1, 0))
      a <- rewriteConcurrentEffects(zio,
                                    beforeEffect = counter.update(incCounter),
                                    onStart = counter.update(incThreads),
                                    onEnd = counter.update(decThreads))
      effects <- counter.get
    } yield (a, effects._2)

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

  def rewriteConcurrentEffects[R, E, A, S](
      zio: ZIO[R, E, A],
      onStart: UIO[Any],
      onEnd: UIO[Any],
      beforeEffect: UIO[Any]): ZIO[R, E, A] = {

    def rec[R2, E1, A1](zio2: ZIO[R2, E1, A1]): ZIO[R2, E1, A1] =
      rewriteConcurrentEffects(zio2, onStart, onEnd, beforeEffect)

    zio match {
      case value: ZIO.Effect[A]         => beforeEffect *> value
      case value: ZIO.EffectAsync[E, A] => beforeEffect *> value
      case value: ZIO.Fork[_, _] =>
        val prepIO =
          onStart *>
            rec(value.value) ensuring
            onEnd

        prepIO.fork

      case value: ZIO.Succeed[A] => value
      case value: ZIO.FlatMap[R, E, _, A] =>
        rec(value.zio).flatMap(x => rec(value.k(x)))
      case value: ZIO.Uninterruptible[R, E, A] =>
        rec(value.zio).uninterruptible
      case value: ZIO.Supervise[R, E, A] =>
        rec(value.value).superviseWith(x => rec(value.supervisor(x)))
      case value: ZIO.Fail[E] => value
      case value: ZIO.Ensuring[R, E, A] =>
        rec(value.zio).ensuring(rec(value.finalizer))
      case d @ ZIO.Descriptor      => d
      case lock: ZIO.Lock[R, E, A] => rec(lock.zio).lock(lock.executor)
      case y @ ZIO.Yield           => y
      case fold: ZIO.Fold[R, E, _, A, _] =>
        rec(fold.value)
          .foldCauseM(fold.err.andThen(rec), fold.succ.andThen(rec))
      case provide: ZIO.Provide[_, E, A] =>
        rec(provide.next).provide(provide.r)
      case read: ZIO.Read[R, E, A] =>
        ZIO.accessM(read.k.andThen(rec))
    }
  }

}
