package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.mc.BehaviorsEquals

case class ActorState(msgs: Map[ActorPath, List[_]], behavior: Behavior[_]) {
  assert(msgs.values.forall(_.nonEmpty))

  override def equals(o: Any): Boolean =
    this == o || (o match {
      case that: ActorState =>
        this.msgs == that.msgs && BehaviorsEquals.areEquivalent(
          this.behavior,
          that.behavior
        )
      case _ => false
    })

  override def hashCode(): Int = msgs.hashCode() ^ behavior.getClass.hashCode()
}
