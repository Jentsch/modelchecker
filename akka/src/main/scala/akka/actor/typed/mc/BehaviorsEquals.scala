package akka.actor.typed.mc
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.{ReceiveImpl, ReceiveMessageImpl}
import berlin.jentsch.modelchecker.akka.ReflectiveEquals

/**
  * @example Empty
  * {{{
  * import akka.actor.typed.scaladsl.Behaviors._
  * import akka.actor.typed.scaladsl.Behaviors
  * import akka.actor.typed.mc.BehaviorsEquals
  *
  * assert(BehaviorsEquals(Behaviors.empty, Behaviors.empty))
  * assert(BehaviorsEquals(stopped, stopped))
  * assert(! BehaviorsEquals(stopped, Behaviors.empty))
  *
  * def receiving = receive[String]{case (ctx, m) => same}
  * assert(receiving != receiving)
  * assert(BehaviorsEquals(receiving, receiving))
  *
  * def receivingMsg = receiveMessage[String]{
  *   case "" => stopped
  *   case _ => same
  * }
  * assert(BehaviorsEquals(receivingMsg, receivingMsg))
  *
  * }}}
  */
object BehaviorsEquals {
  def apply(behavior1: Behavior[_], behavior2: Behavior[_]): Boolean =
    (behavior1, behavior2) match {
      case (`behavior2`, _) => true
      case (rec1: ReceiveImpl[_], rec2: ReceiveImpl[_]) =>
        ReflectiveEquals(rec1.onMessage, rec2.onMessage)
      case (rec1: ReceiveMessageImpl[_], rec2: ReceiveMessageImpl[_]) =>
        ReflectiveEquals(rec1.onMessage, rec2.onMessage)
      case _ => false
    }
}
