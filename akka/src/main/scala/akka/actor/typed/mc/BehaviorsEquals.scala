package akka.actor.typed.mc
import akka.actor.typed.Behavior
import akka.actor.typed.Behavior.{DeferredBehavior, StoppedBehavior}
import akka.actor.typed.scaladsl.Behaviors.{ReceiveImpl, ReceiveMessageImpl}
import berlin.jentsch.modelchecker.akka.ReflectiveEquals
import org.scalactic.Equivalence

/**
  * @example Empty
  * {{{
  * import akka.actor.typed.scaladsl.Behaviors._
  * import akka.actor.typed.scaladsl.Behaviors
  * import akka.actor.typed.mc.BehaviorsEquals
  *
  * assert(BehaviorsEquals.areEquivalent(Behaviors.empty, Behaviors.empty))
  * assert(BehaviorsEquals.areEquivalent(stopped, stopped))
  * assert(! BehaviorsEquals.areEquivalent(stopped, Behaviors.empty))
  *
  * def receiving = receive[String]{case (ctx, m) => same}
  * assert(receiving != receiving)
  * assert(BehaviorsEquals.areEquivalent(receiving, receiving))
  *
  * def receivingMsg = receiveMessage[String]{
  *   case "" => stopped
  *   case _ => same
  * }
  * assert(BehaviorsEquals.areEquivalent(receivingMsg, receivingMsg))
  *
  * }}}
  */
object BehaviorsEquals extends Equivalence[Behavior[_]] {
  override def areEquivalent(
      behavior1: Behavior[_],
      behavior2: Behavior[_]
  ): Boolean =
    (behavior1, behavior2) match {
      case (`behavior2`, _) => true
      case (rec1: ReceiveImpl[_], rec2: ReceiveImpl[_]) =>
        ReflectiveEquals(rec1.onMessage, rec2.onMessage)
      case (_: ReceiveImpl[_], _) => false

      case (rec1: ReceiveMessageImpl[_], rec2: ReceiveMessageImpl[_]) =>
        ReflectiveEquals(rec1.onMessage, rec2.onMessage)
      case (_: ReceiveMessageImpl[_], _) => false

      case (def1: DeferredBehavior[_], def2: DeferredBehavior[_]) =>
        ReflectiveEquals(def1, def2)
      case (_: DeferredBehavior[_], _) => false

      case (stopped1: StoppedBehavior[_], stopped2: StoppedBehavior[_]) =>
        ReflectiveEquals(stopped1.postStop.x, stopped2.postStop.x)
      case (_: StoppedBehavior[_], _) => false

      case (Behavior.EmptyBehavior, _) => behavior2 eq Behavior.EmptyBehavior
    }
}
