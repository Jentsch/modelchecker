package berlin.jentsch.modelchecker.scalaz

import berlin.jentsch.modelchecker.Traverser
import scalaz.zio.{UIO, ZIO}

trait NonDeterministic {
  val nonDeterministic: NonDeterministic.Service[Any]
}

/**
  * @example using NonDeterministic
  * {{{
  * import berlin.jentsch.modelchecker.scalaz.NonDeterministic.doOneOf
  * import scalaz.zio.UIO
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
    def oneOf[E, A](actions: (UIO[Boolean], ZIO[Any, E, A])*): ZIO[R, E, A]
  }

  private[scalaz] class Model(traverser: Traverser) extends NonDeterministic {
    override val nonDeterministic: Service[Any] = new Service[Any] {
      override def oneOf[E, A](
          actions: (UIO[Boolean], ZIO[Any, E, A])*
      ): ZIO[Any, E, A] = {
        for {
          evaluatedGuard <- ZIO.foreach(actions) {
            case (guard, action) =>
              guard.map(_ -> action)
          }
          availableActions = evaluatedGuard.collect {
            case (true, action) => action
          }
          a <- ZIO.effectTotal(traverser.choose(availableActions)).flatten
        } yield a
      }
    }
  }

  def doOneOf[E, A](
      actions: (UIO[Boolean], ZIO[Any, E, A])*
  ): ZIO[NonDeterministic, E, A] =
    ZIO.accessM(_.nonDeterministic.oneOf(actions: _*))
}
