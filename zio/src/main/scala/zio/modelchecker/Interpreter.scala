package zio.modelchecker

import berlin.jentsch.modelchecker.{
  EveryPathTraverser,
  RandomTraverser,
  Traverser
}
import zio._
import zio.internal.{Executor, Platform}

import scala.concurrent.ExecutionContext

class Interpreter(newTraverser: () => Traverser) {

  def apply[E, A](zio: ZIO[NonDeterministic, E, A]): Set[Option[Exit[E, A]]] = {
    val yielding = yieldingEffects(zio)

    val testRuntime = new TestRuntime
    testRuntime.ana(yielding)
  }

  /**
    * Same as [[apply]], but for `ZIO` without any possible error value.
    * Returns a set of possible outcomes of `zio`.
    *
    * {{{
    * import zio.IO
    *
    * val prog = IO.succeed(1) race IO.succeed(1)
    * Interpreter.notFailing(prog) === Set(Some(1), Some(2))
    * }}}
    */
  def notFailing[A](zio: ZIO[NonDeterministic, Nothing, A]): Set[Option[A]] =
    this.apply[Nothing, A](zio).map {
      case Some(Exit.Success(value)) => Some(value)
      case Some(_: Exit.Failure[Nothing]) =>
        sys.error("Should be impossible, since E is Nothing, hence impossible")
      case None => None
    }

  def terminatesAlwaysSuccessfully[A](
      zio: ZIO[NonDeterministic, Nothing, A]
  ): Set[A] =
    notFailing[A](zio).map {
      case Some(value) => value
      case None        => sys.error("Doesn't terminate")
    }

  def terminates[E, A](zio: ZIO[NonDeterministic, E, A]): Set[Exit[E, A]] =
    this.apply[E, A](zio).map {
      case Some(error) => error
      case None        => sys.error("Was not terminated")
    }

  private class TestRuntime extends Runtime[NonDeterministic] {
    private val traverser: Traverser = newTraverser()
    private val pendingRunnables = collection.mutable.Buffer.empty[Runnable]
    private val appendingExecutionContext = new ExecutionContext {
      override def execute(runnable: Runnable): Unit =
        pendingRunnables += runnable
      override def reportFailure(cause: Throwable): Unit = throw cause
    }
    private val neverYieldingExecutor: Executor =
      Executor.fromExecutionContext(Int.MaxValue)(appendingExecutionContext)

    override val Environment: NonDeterministic =
      new NonDeterministic.Model(traverser)

    override val Platform: Platform =
      zio.internal.PlatformLive.fromExecutor(neverYieldingExecutor)

    def ana[E, A](io: ZIO[NonDeterministic, E, A]): Set[Option[Exit[E, A]]] = {
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

  private def yieldingEffects[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    zio match {
      case effect: ZIO.EffectTotal[A]       => ZIO.yieldNow *> effect
      case effect: ZIO.EffectPartial[A]     => ZIO.yieldNow *> effect
      case effect: ZIO.EffectAsync[R, E, A] => ZIO.yieldNow *> effect

      // Don't allow to change executor
      case lock: ZIO.Lock[R, E, A] => yieldingEffects(lock.zio)
      // Recursively apply the rewrite
      case value: ZIO.Succeed[A] => value
      case value: ZIO.Fork[R, _, _] =>
        new ZIO.Fork(yieldingEffects(value.value))
      case value: ZIO.FlatMap[R, E, _, A] =>
        yieldingEffects(value.zio).flatMap(x => yieldingEffects(value.k(x)))
      case value: ZIO.CheckInterrupt[R, E, A] =>
        new ZIO.CheckInterrupt[R, E, A](value.k.andThen(yieldingEffects))
      case value: ZIO.InterruptStatus[R, E, A] =>
        new ZIO.InterruptStatus[R, E, A](yieldingEffects(value.zio), value.flag)
      case status: ZIO.SuperviseStatus[R, E, A] =>
        new ZIO.SuperviseStatus(yieldingEffects(status.value), status.status)
      case fail: ZIO.Fail[E, A]       => fail
      case d: ZIO.Descriptor[R, E, A] => d
      case ZIO.Yield                  => ZIO.Yield
      case fold: ZIO.Fold[R, E, _, A, _] =>
        yieldingEffects(fold.value)
          .foldCauseM(
            fold.failure.andThen(yieldingEffects),
            fold.success.andThen(yieldingEffects)
          )
      case provide: ZIO.Provide[_, E, A] =>
        yieldingEffects(provide.next).provide(provide.r)
      case read: ZIO.Read[R, E, A] =>
        ZIO.accessM(read.k.andThen(yieldingEffects))
      case suspend: ZIO.EffectSuspendTotalWith[R, E, A] =>
        new ZIO.EffectSuspendTotalWith(p => yieldingEffects(suspend.f(p)))
      case suspend: ZIO.EffectSuspendPartialWith[R, A] =>
        new ZIO.EffectSuspendPartialWith(p => yieldingEffects(suspend.f(p)))
      case newFib: ZIO.FiberRefNew[_] =>
        newFib
      case mod: ZIO.FiberRefModify[_, A] =>
        mod
      case ZIO.Trace =>
        ZIO.Trace
      case status: ZIO.TracingStatus[R, E, A] =>
        new ZIO.TracingStatus(yieldingEffects(status.zio), status.flag)
      case check: ZIO.CheckTracing[R, E, A] =>
        new ZIO.CheckTracing(t => yieldingEffects(check.k(t)))
    }

}

object Interpreter extends Interpreter(() => new RandomTraverser(1000)) {
  val everyPath: Interpreter = new Interpreter(() => new EveryPathTraverser)
}
