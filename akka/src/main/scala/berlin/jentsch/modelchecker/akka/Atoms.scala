package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.mc.{BehaviorsEquals, IsDeferredBehavior}
import akka.actor.typed.scaladsl.Behaviors

private[akka] object Atoms {

  def apply(property: Property): Set[Map[ActorPath, ActorState] => Boolean] =
    atoms(property).map {
      case actorIs: ActorIs =>
        state: Map[ActorPath, ActorState] =>
          BehaviorsEquals.areEquivalent(
            state
              .get(actorIs.path)
              .fold[Behavior[_]](Behaviors.stopped)(_.behavior),
            actorIs.behavior
          )
      case ProgressIsPossible =>
        state: Map[ActorPath, ActorState] =>
          state.values.forall(
            actor =>
              actor.messages.isEmpty && IsDeferredBehavior(actor.behavior)
          )
    }

  private def atoms(property: Property): Set[Atomic] = property match {
    case actorIs: ActorIs           => Set(actorIs)
    case ProgressIsPossible         => Set(ProgressIsPossible)
    case AlwaysEventually(property) => atoms(property)
    case AlwaysGlobally(property)   => atoms(property)
    case AlwaysUntil(during, until) => atoms(during) ++ atoms(until)
    case ExistsEventually(property) => atoms(property)
    case ExistsGlobally(property)   => atoms(property)
    case ExistsUntil(during, until) => atoms(during) ++ atoms(until)
    case True                       => Set.empty
    case Not(property)              => atoms(property)
    case And(property1, property2)  => atoms(property1) ++ atoms(property2)
    case Or(property1, property2)   => atoms(property1) ++ atoms(property2)
    case Show(property)             => atoms(property)
  }

}
