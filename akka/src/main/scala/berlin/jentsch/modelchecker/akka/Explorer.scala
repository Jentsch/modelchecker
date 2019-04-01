package berlin.jentsch.modelchecker.akka

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.mutable

class Explorer[State: Equal, Action](
    automaton: Automaton[State, Action],
    property: Property
) {

  private val states = new java.util.Vector[State]()
  private val transitions = mutable.Set.empty[(Int, Int)]
  private val pending = new java.util.Vector[State]()

  pending.add(automaton.state)

  while (!pending.isEmpty) {
    val currentState = pending.remove(0)

    assert(states.asScala.forall(_ =!= currentState))
    states.add(currentState)
    val currentIndex = states.size() - 1

    automaton.state = currentState

    for (action <- automaton.actions) {}
  }

}
