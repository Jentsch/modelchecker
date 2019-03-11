package berlin.jentsch.modelchecker.scalaz

import scalaz.zio._

object Interpreter {

  /**
    * Counts concurrent effects in `zio` to estimate the runtime for finding all interleaving.
    * The result isn't stable.
    *
    * To counts the effects `zio`, more precisely a modification of `zio`, has to be executed.
    *
    * @param zio program to count the effects
    * @tparam R Runtime environment
    * @example One concurrent effect in `prog`
    * {{{
    * import scalaz.zio._
    * import scalaz.zio.duration._
    *
    * val prog = for {
    *   result <- Ref.make("")
    *   _ <- result.set("Two").fork
    *   _ <- IO.unit.delay(1.second)
    *   _ <- result.set("One")
    * } yield ()
    *
    * unsafeRun(Interpreter.concurrentEffectsCounter(prog)) === 1
    * }}}
    * @example No concurrent effects in `prog`
    * {{{
    * import scalaz.zio._
    * import scalaz.zio.duration._
    *
    * val prog = for {
    *   result <- Ref.make("")
    *   one <- IO.succeed(1)
    *   laterTwo <- IO.succeed(one).map(_ * 2).fork
    *   _ <- IO.unit.delay(1.second)
    *   three = one * 3
    *   two <- laterTwo.join
    *   _ <- result.set(three.toString ++ two.toString)
    * } yield () // Drop result
    *
    * unsafeRun(Interpreter.concurrentEffectsCounter(prog)) === 0
    * }}}
    */
  def concurrentEffectsCounter[R, E](zio: ZIO[R, _, _]): ZIO[R, Nothing, Int] =
    concurrentEffectsCounterAndResult(zio).map(_._2)

  def concurrentEffectsCounterAndResult[R, E, A](
      zio: ZIO[R, E, A]): ZIO[R, Nothing, (Either[E, A], Int)] =
    for {
      counter <- Ref.make((1, 0))
      a <- rewriteConcurrentEffects[R, E, A, Unit](
        zio,
        beforeEffect = _ => counter.update(incCounter),
        onStart = counter.update(incThreads).void,
        onEnd = _ => counter.update(decThreads),
        id = { () }).either
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

  private def rewriteConcurrentEffects[R, E, A, ID](
      zio: ZIO[R, E, A],
      onStart: UIO[ID],
      onEnd: ID => UIO[Any],
      beforeEffect: ID => UIO[Any],
      id: ID): ZIO[R, E, A] = {

    def rec[R2, E1, A1](zio2: ZIO[R2, E1, A1]): ZIO[R2, E1, A1] =
      rewriteConcurrentEffects(zio2, onStart, onEnd, beforeEffect, id)

    zio match {
      case value: ZIO.Effect[A]         => beforeEffect(id) *> value
      case value: ZIO.EffectAsync[E, A] => beforeEffect(id) *> value
      case value: ZIO.Fork[_, _] =>
        val prepIO =
          for {
            id <- onStart
            result <- rewriteConcurrentEffects(value.value,
                                               onStart,
                                               onEnd,
                                               beforeEffect,
                                               id)
              .ensuring(onEnd(id))
          } yield result

        prepIO.fork

      // Recursively apply the rewrite
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
