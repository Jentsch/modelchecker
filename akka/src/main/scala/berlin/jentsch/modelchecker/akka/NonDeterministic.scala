package berlin.jentsch.modelchecker.akka

import akka.actor.typed.{ActorSystem, Extension, ExtensionId}

trait NonDeterministic extends Extension {

  def one[T](actions: (() => T)*): T
  def oneOf[T](traversable: Traversable[T]): T
}

object NonDeterministic extends ExtensionId[NonDeterministic] {
  override def createExtension(system: ActorSystem[_]): NonDeterministic =
    sys.error("non determinism can't be used outside of test")
}
