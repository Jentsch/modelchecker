package berlin.jentsch.modelchecker.scalaz

import berlin.jentsch.modelchecker.{RandomTraverser, Traverser}
import scalaz.zio._
import scalaz.zio.internal.{Executor, Platform, PlatformLive}

import scala.concurrent.ExecutionContext

object Interpreter {

  def apply[E, A](zio: ZIO[Unit, E, A]): Set[Option[Exit[E, A]]] = {
    val yielding = yieldingEffects(zio)

    val testRuntime = new TestRuntime
    testRuntime.ana(yielding)
  }

  /**
    * Same as [[apply]], but for `ZIO` without any possible error value.
    * Returns a set of possible outcomes of `zio`.
    *
    * {{{
    * import scalaz.zio.IO
    *
    * val prog = IO.succeed(1) race IO.succeed(1)
    * Interpreter.notFailing(prog) === Set(Some(1), Some(2))
    * }}}
    */
  def notFailing[A](zio: ZIO[Unit, Nothing, A]): Set[Option[A]] =
    Interpreter[Nothing, A](zio).map {
      case Some(Exit.Success(value)) => Some(value)
      case Some(_: Exit.Failure[Nothing]) =>
        sys.error("Should be impossible, since no failure is possible")
      case None => None
    }

  private def yieldingEffects[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    rewriteConcurrentEffects(zio, ZIO.unit, ZIO.unit, ZIO.yieldNow)

  private class TestRuntime extends Runtime[Unit] {
    private val traverser: Traverser = new RandomTraverser(200)
    private val pendingRunnables = collection.mutable.Buffer.empty[Runnable]
    private val appendingExecutionContext = new ExecutionContext {
      override def execute(runnable: Runnable): Unit =
        pendingRunnables += runnable
      override def reportFailure(cause: Throwable): Unit = throw cause
    }
    private val neverYieldingExecutor: Executor =
      Executor.fromExecutionContext(Int.MaxValue)(appendingExecutionContext)

    override val Environment: Unit = ()
    override val Platform: Platform =
      PlatformLive.fromExecutor(neverYieldingExecutor)

    def ana[E, A](io: ZIO[Unit, E, A]): Set[Option[Exit[E, A]]] = {
      var results: Set[Option[Exit[E, A]]] = Set.empty

      do {
        var result: Option[Exit[E, A]] = Option.empty

        unsafeRunAsync(io)(r => result = Option(r))

        while (pendingRunnables.nonEmpty) {
          traverser
            .removeOne(pendingRunnables)
            .run()
        }

        results += result
      } while (traverser.hasMoreOptions())

      results
    }
  }

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
        beforeEffect = counter.update(incCounter),
        onStart = counter.update(incThreads).void,
        onEnd = counter.update(decThreads)).either
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
      onStart: UIO[Any],
      onEnd: UIO[Any],
      beforeEffect: UIO[Any]): ZIO[R, E, A] = {

    def rec[R2, E1, A1](zio2: ZIO[R2, E1, A1]): ZIO[R2, E1, A1] =
      rewriteConcurrentEffects(zio2, onStart, onEnd, beforeEffect)

    zio match {
      case value: ZIO.Effect[A]         => beforeEffect *> value
      case value: ZIO.EffectAsync[E, A] => beforeEffect *> value
      case value: ZIO.Fork[_, _] =>
        val prepIO = onStart *> rec(value.value).ensuring(onEnd)

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
