package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.mc.BehaviorsEquals

import scala.language.implicitConversions

sealed trait Property {
  def unary_! : Property = Not(this)

  def &(that: Property): Property = And(this, that)

  def |(that: Property): Property = And(this, that)

  def ->(that: Property): Property = !this | that

  /** The property will become true */
  def isInevitable: Property = AlwaysEventually(this)

  def show: String = toString
}

private sealed trait Atomic extends Property

/**
  * An atomic property
  */
private case class ActorIs(path: ActorPath, behavior: Behavior[_])
    extends Atomic {

  /**
    *
    * @example considers behaviors equals
    * {{{
    * import akka.actor.typed.scaladsl.Behaviors._
    * def behavior = setup[Unit] { _ => same }
    *
    * assert(behavior != behavior)
    * assert(ActorIs(root, behavior) == ActorIs(root, behavior))
    * }}}
    */
  override def equals(o: Any): Boolean = {
    o match {
      case other: ActorIs =>
        this.path == other.path && BehaviorsEquals.areEquivalent(
          this.behavior,
          other.behavior
        )
      case _ => false
    }
  }

  override def hashCode: Int =
    path.hashCode() ^ behavior.getClass.hashCode()
}

private case object ProgressIsPossible extends Atomic

private case class AlwaysEventually(property: Property) extends Property
private case class AlwaysGlobally(property: Property) extends Property
private case class AlwaysUntil(during: Property, until: Property)
    extends Property

private case class ExistsEventually(property: Property) extends Property
private case class ExistsGlobally(property: Property) extends Property
private case class ExistsUntil(during: Property, until: Property)
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

  def potentially(property: Property): Property =
    ExistsEventually(property)

  def invariantly(property: Property): Property =
    AlwaysGlobally(property)

  def alwaysEventually(property: Property): Property =
    AlwaysEventually(property)

  def progressIsPossible: Property =
    ProgressIsPossible

  implicit def boolToProperty(boolean: Boolean): Property =
    if (boolean) True else Not(True)

  /** Prints the states which fulfill the property during the evaluation */
  def show(property: Property): Property =
    Show(property)

}
