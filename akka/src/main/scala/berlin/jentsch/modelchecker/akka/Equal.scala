package berlin.jentsch.modelchecker.akka
import akka.actor.typed.Behavior
import akka.actor.typed.mc.BehaviorsEquals

trait Equal[T] {
  def equal(a1: T, a2: T): Boolean
}

object Equal {
  implicit val behaviorEqual: Equal[Behavior[_]] = BehaviorsEquals
}
