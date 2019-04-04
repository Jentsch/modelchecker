package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.mc.{BehaviorsEquals, TestSystem}
import akka.actor.typed.scaladsl.Behaviors
import org.scalactic.source.Position
import org.scalatest.FlatSpec

trait AkkaSpec extends FlatSpec with PropertySyntax {

  type SystemState = Map[ActorPath, ActorState]

  private def test(rootBehavior: Behavior[_], properties: Seq[Property])(
      implicit position: Position
  ): Unit = {
    val initialSystemState: SystemState = Map(
      root -> ActorState(rootBehavior)
    )

    val testSystem = TestSystem(initialSystemState)

    val transitions: Graph[SystemState] =
      Graph.explore(Set(initialSystemState)) { s =>
        testSystem.currentState = s

        testSystem.nextStates
      }

    info("Found states: " ++ transitions.nodes.size.toString)

    properties.foreach { property =>
      assert(
        checkProperty(transitions, property).contains(initialSystemState),
        "the property wasn't fulfilled"
      )
    }
  }

  private def checkProperty(
      transitions: Graph[SystemState],
      property: Property
  ): Set[SystemState] = {
    def check(property: Property): Set[SystemState] =
      checkProperty(transitions, property)

    property match {
      case ActorIs(path, behavior) =>
        transitions.nodes.filter { state =>
          val currentBehavior = state
            .get(path)
            .map(_.behavior)
            .getOrElse(Behaviors.stopped)

          BehaviorsEquals.areEquivalent(currentBehavior, behavior)
        }
      case AlwaysEventually(property) => ???
      case AlwaysNext(property) =>
        transitions.directAncestors(check(property))
      case ExistsEventually(property) => ???
      case ExistsNext(property) =>
        transitions.directAncestors(check(property))
      case Not(property) =>
        transitions.nodes -- check(property)
      case And(property1, property2) =>
        check(property1) intersect check(property2)
    }
  }

  implicit class BehaviorShould(behavior: Behavior[_]) {
    def should(description: String): InWord = new InWord(behavior, description)
  }

  class InWord(behavior: Behavior[_], description: String) {
    def in(properties: Property*)(implicit position: Position): Unit =
      it should description in {
        test(behavior, properties)
      }
  }

}
