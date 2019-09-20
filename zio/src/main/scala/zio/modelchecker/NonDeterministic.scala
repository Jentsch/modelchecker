package zio.modelchecker

import berlin.jentsch.modelchecker.Traverser
import zio.{UIO, ZIO}

trait NonDeterministic {
  val nonDeterministic: NonDeterministic.Service[Any]
}

/**
  * @example using NonDeterministic
  * {{{
  * import zio.modelchecker.NonDeterministic.doOneOf
  * import zio.UIO
  *
  * val program = doOneOf(
  *   UIO.succeed(true) -> UIO.succeed(1),
  *   UIO.succeed(true) -> UIO.succeed(2),
  *   UIO.succeed(false) -> UIO.succeed(3)
  * )
  *
  * Interpreter.terminatesAlwaysSuccessfully(program) should be(Set(1, 2))
  * }}}
  */
object NonDeterministic {
  trait Service[R] {
    def oneOf[A](options: Seq[A]): ZIO[R, Nothing, A]
    def doOneOf[E, A](actions: (UIO[Boolean], ZIO[Any, E, A])*): ZIO[R, E, A]
  }

  private[modelchecker] class Model(traverser: Traverser)
      extends NonDeterministic {
    override val nonDeterministic: Service[Any] = new Service[Any] {
      override def oneOf[A](options: Seq[A]): UIO[A] =
        ZIO.effectTotal(traverser.choose(options))

      override def doOneOf[E, A](
          actions: (UIO[Boolean], ZIO[Any, E, A])*
      ): ZIO[Any, E, A] = {
        for {
          evaluatedGuard <- ZIO.foreach(actions) {
            case (guard, action) => guard.map(_ -> action)
          }
          availableActions = evaluatedGuard.collect {
            case (true, action) => action
          }
          a <- oneOf(availableActions).flatten
        } yield a
      }
    }
  }

  def doOneOf[E, A](
      actions: (UIO[Boolean], ZIO[Any, E, A])*
  ): ZIO[NonDeterministic, E, A] =
    ZIO.accessM(_.nonDeterministic.doOneOf(actions: _*))

  def doAnyOf[E, A](actions: ZIO[Any, E, A]*): ZIO[NonDeterministic, E, A] =
    oneOf(actions).flatten

  def oneOf[A](options: Seq[A]): ZIO[NonDeterministic, Nothing, A] =
    ZIO.accessM(_.nonDeterministic.oneOf(options))
}
