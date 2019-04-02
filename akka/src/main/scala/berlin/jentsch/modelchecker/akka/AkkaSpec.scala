package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.mc.BehaviorsEquals
import org.scalactic.source.Position
import org.scalatest.{Assertion, FlatSpec}

import scala.collection.mutable

trait AkkaSpec extends FlatSpec with PropertySyntax {

  type SystemState = Map[ActorPath, ActorState]

  private def test(rootBehavior: Behavior[_], property: Property)(
      implicit position: Position
  ): Assertion = {
    val initialSystemState: SystemState = Map(
      root -> ActorState(Nil, rootBehavior)
    )

    val unvisisted: mutable.Set[SystemState] = mutable.Set(initialSystemState)

    val visisted: mutable.Set[SystemState] = mutable.Set.empty

    val transitions: Graph[SystemState] = Graph.singleton(initialSystemState)

    while (unvisisted.nonEmpty) {
      val current = unvisisted.head
      unvisisted.remove(current)

      visisted.add(current)
    }

    assert(
      checkProperty(transitions, property).contains(initialSystemState),
      "the property wasn't fulfilled"
    )
  }

  private def checkProperty(
      transitions: Graph[SystemState],
      property: Property
  ): Set[SystemState] =
    property match {
      case ActorIs(path, behavior) =>
        transitions.nodes.filter(
          s => BehaviorsEquals.equal(s(path).behavior, behavior)
        )
      case AlwaysEventually(property) => ???
      case ExistsEventually(property) => ???
      case Not(property) =>
        transitions.nodes -- checkProperty(transitions, property)
    }

  implicit class BehaviorShould(behavior: Behavior[_]) {
    def should(description: String): InWord = new InWord(behavior, description)
  }

  class InWord(behavior: Behavior[_], description: String) {
    def in(property: Property): Unit =
      it should description in {
        test(behavior, property)
      }
  }

}
