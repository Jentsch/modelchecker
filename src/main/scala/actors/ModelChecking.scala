package actors

import scalax.collection.GraphEdge.DiEdge
import scalax.collection._

trait ModelChecking extends ActorSystem {

  type Queues = Map[Actor, List[Message]]
  type SystemState = Map[Actor, ActorState]
  type ActorState = (Behaviour, Queues)

  val EmptyQueues: Queues = Map.empty[Actor, List[Message]]

  def check = {
    val g = graph
    import g._

    nodes foreach println

    (nodes.size, edges.size)
  }

  def initialState: SystemState = {
    val emptyState: SystemState = Map.empty
    initialActors.foldLeft(emptyState){
      case (state, actor) => applyEffects(state, actor.processCreation)
    }
  }

  private def applyEffects(state: SystemState, effects: Effects): SystemState = {
    val Effects(sender, behaviour, messages, actors) = effects
    val (_, queues: Queues) = state.getOrElse(sender, (behaviour, Nil))

    val localEffects: SystemState =
      state + (effects.of ->(behaviour, queues))

    val newActors =
      localEffects ++ actors.map(actor => actor ->(actor.init, EmptyQueues))

    val newMessages: SystemState = messages.foldLeft(newActors) {
      case (state: SystemState, (message, to)) =>
        val (toBehaviour, toQueues) = state(to)
        val newFromQueue = toQueues.getOrElse(sender, Nil) :+ message
        state + (to ->(toBehaviour, toQueues + (sender -> newFromQueue)))
    }

    newMessages
  }

  def graph: Graph[SystemState, DiEdge] =
    recGraph(List(initialState), Graph.empty[SystemState, DiEdge], Set.empty)

  private def evolve(state: SystemState): List[SystemState] = {
    import scala.collection.mutable.ListBuffer
    val buffer = ListBuffer.empty[SystemState]
    for (
      (actor, (behaviour, queues)) <- state;
      (from, msg :: msgs) <- queues
    ) {
      val effects = actor.process(behaviour, msg)
      val Effects(_, newBehaviour, messages, actors) = effects

      val localEffects =
        state + (actor ->(newBehaviour, queues + (from -> msgs)))

      val newActors =
        localEffects ++ actors.map(actor => actor ->(actor.init, EmptyQueues))

      val newMessages = messages.foldLeft(newActors) {
        case (state, (message, to)) =>
          val (toBehaviour, toQueues) = state(to)
          val newFromQueue = toQueues.getOrElse(actor, Nil) :+ message
          state + (to ->(toBehaviour, toQueues + (actor -> newFromQueue)))
      }

      buffer += newMessages

      if (unsafeMessageTransport(msg))
        buffer += state + (actor ->(behaviour, queues + (from -> msgs)))
    }
    buffer.toList
  }

  //TODO: Replace this dirty trick to simulate package lost by a proper data type checked implementation
  private def unsafeMessageTransport(msg: Message): Boolean =
    msg.headOption.exists(_.isUpper)

  private def init: SystemState = Map.empty

  private def recGraph(unvisited: List[SystemState], graph: Graph[SystemState, DiEdge], visited: Set[SystemState]): Graph[SystemState, DiEdge] =
    unvisited match {
      case Nil =>
        graph
      case alreadyVisited :: unvisited if visited(alreadyVisited) =>
        recGraph(unvisited, graph, visited)
      case state :: unvisited if !visited(state) =>
        val reachable = evolve(state)

        // This should be replaced by generic CTL queries
        if (reachable.isEmpty)
          println("Terminal state found")
        val newGraph = graph ++ reachable.map(target => DiEdge(state, target))
        val newUnvisited = reachable.filterNot(visited) ::: unvisited
        recGraph(newUnvisited, newGraph, visited + state)
    }

}
