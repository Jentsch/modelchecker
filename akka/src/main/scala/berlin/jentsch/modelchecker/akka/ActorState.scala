package berlin.jentsch.modelchecker.akka

import akka.actor.typed.Behavior
import akka.actor.typed.mc.BehaviorsEquals

case class ActorState(msg: List[_], behavior: Behavior[_]) {
  override def equals(o: Any): Boolean =
    this == o || (o match {
      case that: ActorState =>
        this.msg == that.msg && BehaviorsEquals.equal(
          this.behavior,
          that.behavior
        )
      case _ => false
    })
}
