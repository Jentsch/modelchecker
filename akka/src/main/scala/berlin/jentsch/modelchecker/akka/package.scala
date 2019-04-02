package berlin.jentsch.modelchecker

import _root_.akka.actor.{ActorPath, Address, RootActorPath}

package object akka {
  def root: ActorPath = RootActorPath(Address("test", "TestActorSystem"))
}
