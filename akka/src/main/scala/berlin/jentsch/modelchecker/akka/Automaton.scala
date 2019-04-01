package berlin.jentsch.modelchecker.akka

trait Automaton[State, Action] {
  def state: State
  def state_=(state: State): Unit

  def actions: Iterable[Action]
  def runAction(action: Action)
}
