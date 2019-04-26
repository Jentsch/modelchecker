package akka.actor.typed.mc

import akka.actor.typed.Behavior
import akka.actor.typed.Behavior.DeferredBehavior

object IsDeferredBehavior {
  def apply(behavior: Behavior[_]): Boolean =
    behavior.isInstanceOf[DeferredBehavior[_]]
}
