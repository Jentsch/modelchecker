package berlin.jentsch.modelchecker

import _root_.akka.actor.{ActorPath, Address, RootActorPath}

package object akka {
  def root: ActorPath = RootActorPath(Address("test", "TestActorSystem"))

  private[akka] implicit class EqualitySyntax[T](val t: T) extends AnyVal {
    def ===(other: T)(implicit equal: Equal[T]): Boolean =
      equal.equal(t, other)
    def =!=(other: T)(implicit equal: Equal[T]): Boolean =
      !equal.equal(t, other)
  }
}
