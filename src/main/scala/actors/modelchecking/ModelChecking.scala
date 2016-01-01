package actors.modelchecking

import actors.ActorSystem

trait ModelChecking extends ActorSystem {

  type Queues = Map[Actor, List[Message]]
  type ActorState = (Behaviour, Queues)
  type SystemState = Map[Actor, ActorState]
  type States = Set[SystemState]

  val pathTraverser = new PathTraverser

  val EmptyQueues: Queues = Map.empty[Actor, List[Message]]

  class Result(val graph: Graph[SystemState], val initialStates: States) {
    /** All reachable states */
    private val omega: States = graph.nodes
    require(omega.nonEmpty)

    /**
     * Atomic expression on actors
     */
    implicit class ActorExpression(val actor: Actor) {

      /** All states where the actor has the given behaviour */
      def is(behaviour: Behaviour): States =
        omega filter { state => state(actor)._1 == behaviour }

      /** All states where the actor could process the message in the next iteration */
      def receive(msg: Message): States =
        omega filter { state =>
          val (_, queues) = state(actor)
          queues.values.exists(_.headOption == Some(msg))
        }
    }

    implicit class StateExpression(val self: States) {
      // all other operations like & (and, intersection), | (or, union) and &~ (xor, diff) are already defined for sets

      def unary_! : States =
        omega -- self

      def ->(other: States): States =
        (!self) | other

      def existsUntil(other: States) =
        graph.withAncestors(other, self)
    }

    def alwaysGlobaly(states: States): States =
      ???

    final def existsEventually(states: States): States =
      graph withAncestors states
    final def EF(states: States) =
      existsEventually(states)

    final def alwaysGlobally(states: States): States =
      EF(!states)
    final def AG(states: States) =
      alwaysGlobally(states)

    def assume(assumptions: States*) = {
      assumptions forall { assumption =>
        initialStates.subsetOf(assumption)
      }
    }

  }

  def check = {
    val init = List(initialState)
    val graph = Graph.explore(init)(evolve)

    new Result(graph, init.to[Set])
  }

  def initialState: SystemState = {
    val emptyState: SystemState = Map.empty
    initialActors.foldLeft(emptyState) {
      case (state, actor) => applyEffects(state, actor.processCreation)
    }
  }

  private def applyEffects(state: SystemState, effects: Effects): SystemState = {
    val Effects(sender, behaviour, messages, actors) = effects
    val (_, queues: Queues) = state.getOrElse(sender, (behaviour, Map.empty))

    val localEffects: SystemState =
      state + (effects.of -> (behaviour, queues))

    val newActors =
      localEffects ++ actors.map(actor => actor -> (actor.init, EmptyQueues))

    val newMessages: SystemState = messages.foldLeft(newActors) {
      case (state: SystemState, (message, to)) =>
        val (toBehaviour, toQueues) = state(to)
        val newFromQueue = toQueues.getOrElse(sender, Nil) :+ message
        state + (to -> (toBehaviour, toQueues + (sender -> newFromQueue)))
    }

    newMessages
  }

  /**
   * Returns all direct successor states
   */
  private def evolve(state: SystemState): List[SystemState] = {
    import scala.collection.mutable.ListBuffer
    val buffer = ListBuffer.empty[SystemState]
    for (
      (actor, (behaviour, queues)) <- state;
      (from, msg :: msgs) <- queues
    ) {
      pathTraverser.reset()

      do {
        val effects = actor.process(behaviour, msg)
        val Effects(_, newBehaviour, messages, actors) = effects

        val localEffects =
          state + (actor -> (newBehaviour, queues + (from -> msgs)))

        val newActors =
          localEffects ++ actors.map(actor => actor -> (actor.init, EmptyQueues))

        val newMessages = messages.foldLeft(newActors) {
          case (state, (message, to)) =>
            val (toBehaviour, toQueues) = state(to)
            val newFromQueue = toQueues.getOrElse(actor, Nil) :+ message
            state + (to -> (toBehaviour, toQueues + (actor -> newFromQueue)))
        }

        buffer += newMessages

        if (unsafeMessageTransport(msg))
          buffer += state + (actor -> (behaviour, queues + (from -> msgs)))
      } while (pathTraverser.chooseNext())
    }
    buffer.toList
  }

  /**
    * Choose none deterministic one of the given options
    */
  def choose[T](head: T, tail: T*): T = {
    pathTraverser.choose(head +: tail)
  }

  //TODO: Replace this dirty trick to simulate package lost by a proper data type checked implementation or
  // configuration
  private def unsafeMessageTransport(msg: Message): Boolean =
    msg.headOption.exists(_.isUpper)

}
