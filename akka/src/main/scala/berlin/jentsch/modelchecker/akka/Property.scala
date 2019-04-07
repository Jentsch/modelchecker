package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior

import scala.language.implicitConversions

sealed trait Property {
  def unary_! : Property = Not(this)

  def &(that: Property): Property = And(this, that)

  def |(that: Property): Property = And(this, that)

  def show = toString
}

/**
  * An atomic property
  */
private case class ActorIs(path: ActorPath, behavior: Behavior[_])
    extends Property

private case object ProgressIsPossible extends Property

private case class AlwaysEventually(property: Property) extends Property
private case class AlwaysGlobally(property: Property) extends Property
private case class AlwaysUntil(property1: Property, property2: Property)
    extends Property

private case class ExistsEventually(property: Property) extends Property
private case class ExistsUntil(property1: Property, property2: Property)
    extends Property

/** @group Bool */
private case object True extends Property

/** @group Bool */
private case class Not(property: Property) extends Property {
  override def show: String = "!" ++ property.show
}

/** @group Bool */
private case class And(property1: Property, property2: Property)
    extends Property

/** @group Bool */
private case class Or(property1: Property, property2: Property) extends Property

private case class Show(property: Property) extends Property {
  override def show: String = property.show
}

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

  def alwaysGlobally(property: Property): Property =
    AlwaysGlobally(property)

  def progressIsPossible: Property =
    ProgressIsPossible

  implicit def boolToProperty(boolean: Boolean): Property =
    if (boolean) True else Not(True)

  def existsUntil(property1: Property, property2: Property): Property =
    ExistsUntil(property1, property2)

  def alwaysUntil(property1: Property, property2: Property): Property =
    AlwaysUntil(property1, property2)

  /** Prints the states which fulfill the property during the evaluation */
  def show(property: Property): Property =
    Show(property)

}
