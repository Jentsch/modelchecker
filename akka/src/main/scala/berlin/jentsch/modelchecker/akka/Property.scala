package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior

sealed trait Property {
  def unary_! : Property = Not(this)

  def &(that: Property): Property = And(this, that)
}

/**
  * An atomic property
  */
private case class ActorIs(path: ActorPath, behavior: Behavior[_])
    extends Property

private case class AlwaysEventually(property: Property) extends Property
private case class ExistsEventually(property: Property) extends Property

private case class Not(property: Property) extends Property
private case class And(property1: Property, property2: Property)
    extends Property

trait PropertySyntax {

  implicit class PathSyntax(path: ActorPath) {

    /**
      * An atomic property of a single actor.
      * Use Behavior.stopped to test if an actor is stopped or not jet created
      */
    def is(behavior: Behavior[_]): Property = ActorIs(path, behavior)
  }

  def root: ActorPath = berlin.jentsch.modelchecker.akka.root

  def existsEventually(property: Property): Property =
    ExistsEventually(property)

  def alwaysEventually(property: Property): Property =
    AlwaysEventually(property)
}
