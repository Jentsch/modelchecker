package ecspec

/**
  * A traverser that accumulates some state S during the a run and has a initial state for every run. The
  * usage is similar to the normal [[Traverser]] but all chooise* methods require additional information about
  * the associated state
  *
  * @example
  * {{{
  *   val trav = new TrackingTraverser(0)(_ + _)
  *
  *   do {
  *     val a = trav.choose(Seq(1 -> "1", 2 -> "2", 3 -> "3"))
  *     val b = trav.choose(Seq(1 -> "1", 2 -> "2"))
  *
  *     a.toInt + b.toInt should be(trav.state)
  *   } while (trav.hasMoreOptions)
  * }}}
  *
  * Note: init and combine should form a monoid
  */
private[ecspec] class TrackingTraverser[S](init: S)(combine: (S, S) => S) {

  private val traverser = new Traverser

  private var _state: S = init

  def state: S = _state

  def choose[E](choices: Seq[(S, E)]): E = {
    val (newState, result) = traverser.choose(choices)

    _state = combine(_state, newState)

    result
  }

  def hasMoreOptions: Boolean =
    if (traverser.hasMoreOptions) {
      _state = init
      true
    } else {
      false
    }
}
