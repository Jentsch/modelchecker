package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior

sealed trait Property {
  def prefix_- : Property = Not(this)
}

/**
  * An atomic property
  */
case class ActorIs(path: ActorPath, behavior: Behavior[_]) extends Property

case class AlwaysEventually(property: Property) extends Property
case class ExistsEventually(property: Property) extends Property

case class Not(property: Property) extends Property

trait PropertySyntax {

  implicit class PathSyntax(path: ActorPath) {
    def is(behavior: Behavior[_]): Property = ActorIs(path, behavior)
  }

  def root: ActorPath = berlin.jentsch.modelchecker.akka.root

  def existsEventually(property: Property): Property =
    ExistsEventually(property)

  def alwaysEventually(property: Property): Property =
    AlwaysEventually(property)
}
