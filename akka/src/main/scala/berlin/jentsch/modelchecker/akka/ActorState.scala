package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.mc.BehaviorsEquals

case class ActorState(
    behavior: Behavior[_],
    messages: Map[ActorPath, List[_]] = Map.empty.withDefaultValue(Nil)
) { self =>
  assert(messages.values.forall(_.nonEmpty))

  override def equals(o: Any): Boolean =
    o match {
      case that: ActorState =>
        this.messages == that.messages && BehaviorsEquals.areEquivalent(
          this.behavior,
          that.behavior
        )
      case _ => false
    }

  override def hashCode(): Int =
    messages.hashCode() ^ behavior.getClass.hashCode()
}
