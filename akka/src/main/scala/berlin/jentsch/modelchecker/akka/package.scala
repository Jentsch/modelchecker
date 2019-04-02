package berlin.jentsch.modelchecker

import _root_.akka.actor.{ActorPath, Address, RootActorPath}
import scalaz.Order

package object akka {
  def root: ActorPath = RootActorPath(Address("test", "TestActorSystem"))

  implicit val ActorPathOrder: Order[ActorPath] = Order.fromScalaOrdering
}
