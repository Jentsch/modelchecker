package actors

import scalax.collection.GraphEdge.DiEdge
import scalax.collection._

trait ModelChecking extends ActorSystem {

  type Queues = Map[Actor, List[Message]]
  type State = Map[Actor, (Behaviour, Queues)]
  val EmptyQueues: Queues = Map.empty[Actor, List[Message]]

  def check = {
    val g = graph
    import g._

    nodes foreach println

    (nodes.size, edges.size)
  }

  def initialState: State = {
    val emptyState: State = Map.empty
    initialActors.foldLeft(emptyState){
      case (state, actor) => applyEffects(state, actor.processCreation)
    }
  }

  private def applyEffects(state: State, effects: Effects): State = {
    val Effects(sender, behaviour, messages, actors) = effects
    val (_, queues: Queues) = state.getOrElse(sender, (behaviour, Nil))

    val localEffects: State =
      state + (effects.of ->(behaviour, queues))

    val newActors =
      localEffects ++ actors.map(actor => actor ->(actor.init, EmptyQueues))

    val newMessages: State = messages.foldLeft(newActors) {
      case (state: State, (message, to)) =>
        val (toBehaviour, toQueues) = state(to)
        val newFromQueue = toQueues.getOrElse(sender, Nil) :+ message
        state + (to ->(toBehaviour, toQueues + (sender -> newFromQueue)))
    }

    newMessages
  }

  def graph: Graph[State, DiEdge] =
    recGraph(List(initialState), Graph.empty[State, DiEdge], Set.empty)

  private def envolve(state: State): List[State] = {
    import scala.collection.mutable.ListBuffer
    val buffer = ListBuffer.empty[State]
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
    msg.headOption.map(_.isUpper).getOrElse(false)


  private def init: State = Map.empty

  private def recGraph(unvisited: List[State], graph: Graph[State, DiEdge], visited: Set[State]): Graph[State, DiEdge] =
    unvisited match {
      case Nil =>
        graph
      case alreadyVisited :: unvisited if visited(alreadyVisited) =>
        recGraph(unvisited, graph, visited)
      case state :: unvisited if !visited(state) =>
        val reachable = envolve(state)

        // This should be replaced by generic CTL queries
        if (reachable.isEmpty)
          println("Terminal state found")
        val newGraph = graph ++ reachable.map(target => DiEdge(state, target))
        val newUnvisited = reachable.filterNot(visited) ::: unvisited
        recGraph(newUnvisited, newGraph, visited + state)
    }

}
