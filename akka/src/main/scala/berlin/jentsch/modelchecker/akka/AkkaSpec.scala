package berlin.jentsch.modelchecker.akka

import akka.actor.ActorPath
import akka.actor.typed.Behavior
import akka.actor.typed.mc.{BehaviorsEquals, TestSystem}
import akka.actor.typed.scaladsl.Behaviors
import org.scalactic.source.Position
import org.scalatest.FlatSpec
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

trait AkkaSpec extends FlatSpec with PropertySyntax {

  type SystemState = Map[ActorPath, ActorState]

  private def test(rootBehavior: Behavior[_], properties: Seq[Property])(
      implicit position: Position
  ): Unit = {
    val initialSystemState: SystemState = Map(
      root -> ActorState(rootBehavior)
    )

    val testSystem = TestSystem(initialSystemState)

    val transitions: Graph[SystemState, DiEdge] =
      GraphExplorer.explore(Set(initialSystemState)) { s =>
        testSystem.currentState = s

        testSystem.nextStates
      }

    info("Found states: " ++ transitions.nodes.size.toString)

    properties.foreach { property =>
      if (checkProperty(transitions, property).forall(
            _.value != initialSystemState
          )) {
        fail(s"The property ${property.show} wasn't fulfilled")
      }
      assert(
        checkProperty(transitions, property)
          .exists(_.value == initialSystemState),
        "the property wasn't fulfilled"
      )
    }
  }

  private def checkProperty(
      transitions: Graph[SystemState, DiEdge],
      property: Property
  ): collection.Set[transitions.NodeT] = {
    def check(property: Property): collection.Set[transitions.NodeT] =
      checkProperty(transitions, property)

    property match {
      case ActorIs(path, behavior) =>
        require(
          !behavior.getClass.getSimpleName.contains("DeferredBehavior"),
          "Test for DeferredBehavior is not supported! This could be a false error!"
        )

        transitions.nodes.filter { state =>
          val currentBehavior = state.value
            .get(path)
            .map(_.behavior)
            .getOrElse(Behaviors.stopped)

          BehaviorsEquals.areEquivalent(currentBehavior, behavior)
        }
      case ProgressIsPossible =>
        transitions.nodes.filter(_.diSuccessors.nonEmpty)
      case Not(ProgressIsPossible) =>
        transitions.nodes.filter(_.diSuccessors.isEmpty)
      case ExistsEventually(property) =>
        @scala.annotation.tailrec
        def expand(
            set: collection.Set[transitions.NodeT]
        ): collection.Set[transitions.NodeT] = {
          val bigger = set.flatMap(_.diPredecessors) ++ set

          if (bigger.size == set.size)
            bigger
          else
            expand(bigger)
        }

        expand(check(property))
      case ExistsEventually(property) =>
        @scala.annotation.tailrec
        def expand(
            set: collection.Set[transitions.NodeT]
        ): collection.Set[transitions.NodeT] = {
          val bigger = set.flatMap(_.diPredecessors) ++ set

          if (bigger.size == set.size)
            bigger
          else
            expand(bigger)
        }

        expand(check(property))

      case AlwaysNext(property) =>
        check(Not(ExistsNext(Not(property))))

      case AlwaysGlobally(property) =>
        @scala.annotation.tailrec
        def expand(
            set: collection.Set[transitions.NodeT]
        ): collection.Set[transitions.NodeT] = {
          val bigger = transitions.nodes.filter(_.diSuccessors.subsetOf(set)) ++ set

          if (bigger.size == set.size)
            bigger
          else
            expand(bigger)
        }

        expand(check(property))

      case ExistsNext(property) =>
        check(property).flatMap(_.diPredecessors)
      case AlwaysNext(property) =>
        val succ = check(property)
        transitions.nodes.filter(_.diSuccessors.subsetOf(succ))

      case Not(property) =>
        transitions.nodes -- check(property)
      case And(property1, property2) =>
        check(property1) intersect check(property2)
      case Or(property1, property2) =>
        check(property1) ++ check(property2)
      case True =>
        transitions.nodes

      case Show(property) =>
        println(property.show)
        val pr = check(property)
        println(pr.toString)
        pr
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
