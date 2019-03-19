package berlin.jentsch.modelchecker.scalaz

import berlin.jentsch.modelchecker.{RandomTraverser, Traverser}
import scalaz.zio._
import scalaz.zio.internal.{Executor, Platform, PlatformLive}
import scalaz.zio.random.Random

import scala.concurrent.ExecutionContext

/**
  * @example using random
  * {{{
  * import scalaz.zio.random
  *
  * Interpreter.terminatesAlwaysSuccessfully(random.nextInt(3)) should be(Set(0, 1, 2))
  * }}}
  */
object Interpreter {

  def apply[E, A](zio: ZIO[Random, E, A]): Set[Option[Exit[E, A]]] = {
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
  def notFailing[A](zio: ZIO[Random, Nothing, A]): Set[Option[A]] =
    Interpreter[Nothing, A](zio).map {
      case Some(Exit.Success(value)) => Some(value)
      case Some(_: Exit.Failure[Nothing]) =>
        sys.error("Should be impossible, since no failure is possible")
      case None => None
    }

  def terminatesAlwaysSuccessfully[A](zio: ZIO[Random, Nothing, A]): Set[A] =
    notFailing[A](zio).map {
      case Some(value) => value
      case None        => sys.error("Doesn't terminate")
    }

  private class TestRuntime extends Runtime[Random] {
    private val traverser: Traverser = new RandomTraverser(200)
    private val pendingRunnables = collection.mutable.Buffer.empty[Runnable]
    private val appendingExecutionContext = new ExecutionContext {
      override def execute(runnable: Runnable): Unit =
        pendingRunnables += runnable
      override def reportFailure(cause: Throwable): Unit = throw cause
    }
    private val neverYieldingExecutor: Executor =
      Executor.fromExecutionContext(Int.MaxValue)(appendingExecutionContext)

    override val Environment: Random = new Random {
      override val random: Random.Service[Any] with Object =
        new Random.Service[Any] {
          override val nextBoolean =
            ZIO.effectTotal(traverser.choose(Seq(true, false)))
          override def nextBytes(length: Int) = ???
          override val nextDouble =
            ZIO.succeedLazy(sys.error("Not implemented"))
          override val nextFloat = ZIO.effectTotal(sys.error("Not implemented"))
          override val nextGaussian =
            ZIO.effectTotal(sys.error("Not implemented"))
          override def nextInt(n: Int) =
            ZIO.effectTotal(traverser.choose(0 until n))
          override val nextInt =
            ZIO.effectTotal(traverser.choose(Int.MinValue until Int.MaxValue))
          override val nextLong =
            ZIO.effectTotal(traverser.choose(Long.MinValue until Long.MaxValue))
          override val nextPrintableChar =
            ZIO.effectTotal(sys.error("Not implemented"))
          override def nextString(length: Int) =
            ZIO.effectTotal(sys.error("Not implemented"))
        }
    }
    override val Platform: Platform =
      PlatformLive.fromExecutor(neverYieldingExecutor)

    def ana[E, A](io: ZIO[Random, E, A]): Set[Option[Exit[E, A]]] = {
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

  private def yieldingEffects[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] = {

    zio match {
      case value: ZIO.Effect[A]         => ZIO.yieldNow *> value
      case value: ZIO.EffectAsync[E, A] => ZIO.yieldNow *> value

      // Recursively apply the rewrite
      case value: ZIO.Succeed[A] => value
      case value: ZIO.Fork[_, _] =>
        yieldingEffects(value.value).fork
      case value: ZIO.FlatMap[R, E, _, A] =>
        yieldingEffects(value.zio).flatMap(x => yieldingEffects(value.k(x)))
      case value: ZIO.Uninterruptible[R, E, A] =>
        yieldingEffects(value.zio).uninterruptible
      case value: ZIO.Supervise[R, E, A] =>
        yieldingEffects(value.value).superviseWith(x =>
          yieldingEffects(value.supervisor(x)))
      case fail: ZIO.Fail[E] => fail
      case value: ZIO.Ensuring[R, E, A] =>
        yieldingEffects(value.zio).ensuring(yieldingEffects(value.finalizer))
      case d @ ZIO.Descriptor => d
      // Don't allow to change executor
      case lock: ZIO.Lock[R, E, A] => yieldingEffects(lock.zio)
      case y @ ZIO.Yield           => y
      case fold: ZIO.Fold[R, E, _, A, _] =>
        yieldingEffects(fold.value)
          .foldCauseM(fold.err.andThen(yieldingEffects),
                      fold.succ.andThen(yieldingEffects))
      case provide: ZIO.Provide[_, E, A] =>
        yieldingEffects(provide.next).provide(provide.r)
      case read: ZIO.Read[R, E, A] =>
        ZIO.accessM(read.k.andThen(yieldingEffects))
    }
  }

}
