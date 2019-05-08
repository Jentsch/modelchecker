package berlin.jentsch.modelchecker.scalaz

import java.util.WeakHashMap

import _root_.scalaz.zio.Exit.Cause
import _root_.scalaz.zio._
import _root_.scalaz.zio.internal.{Executor, Platform}
import berlin.jentsch.modelchecker.{EveryPathTraverser, RandomTraverser, Traverser, scalaz}

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
    * import scalaz.zio.IO
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

  def terminatesAlwaysSuccessfully[A](zio: ZIO[NonDeterministic, Nothing, A]): Set[A] =
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

    override val Environment: NonDeterministic = new scalaz.NonDeterministic.Model(traverser)
    override val Platform: Platform = new Platform {
      val executor = neverYieldingExecutor

      def nonFatal(t: Throwable): Boolean =
        !t.isInstanceOf[VirtualMachineError]

      def reportFailure(cause: Cause[_]): Unit =
        ()

      def newWeakHashMap[A, B]() =
        new WeakHashMap[A, B]()
    }

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
      case supervised: ZIO.Supervised[R, E, A] =>
        yieldingEffects(supervised.value).supervised
      case fail: ZIO.Fail[E] => fail
      case value: ZIO.Ensuring[R, E, A] =>
        yieldingEffects(value.zio).ensuring(yieldingEffects(value.finalizer))
      case d @ ZIO.Descriptor => d
      // Don't allow to change executor
      case lock: ZIO.Lock[R, E, A] => yieldingEffects(lock.zio)
      case y @ ZIO.Yield           => y
      case fold: ZIO.Fold[R, E, _, A, _] =>
        yieldingEffects(fold.value)
          .foldCauseM(
            fold.err.andThen(yieldingEffects),
            fold.succ.andThen(yieldingEffects)
          )
      case provide: ZIO.Provide[_, E, A] =>
        yieldingEffects(provide.next).provide(provide.r)
      case read: ZIO.Read[R, E, A] =>
        ZIO.accessM(read.k.andThen(yieldingEffects))
    }
  }

}

object Interpreter extends Interpreter(() => new RandomTraverser(1000)) {
  val everyPath: Interpreter = new Interpreter(() => new EveryPathTraverser)
}
