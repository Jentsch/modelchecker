package ecspec

/**
  * A traverser that tracks the probability of the current state. Probability is modeled as a Double in the range of 0.0 ... 1.0.
  *
  * {{{
  *   val trav = new ProbabilityTraverser
  *
  *   do {
  *     val rolled1 = trav.choose(Seq(1.0/6.0 -> "1", 5.0/6.0 -> "2-6")) // roll a dice
  *     val rolled2 = trav.choose(Seq(1.0/6.0 -> "1", 5.0/6.0 -> "2-6"))
  *
  *     (rolled1 + " & " + rolled2) match {
  *       case "1 & 1" =>
  *         trav.probability should be(1.0/36.0 +- 0.0001)
  *       case "2-6 & 2-6" =>
  *         trav.probability should be(25.0/36.0 +- 0.0001)
  *       case "1 & 2-6" =>
  *         trav.probability should be(5.0/36.0 +- 0.0001)
  *       case "2-6 & 1" =>
  *         trav.probability should be(5.0/36.0 +- 0.0001)
  *     }
  *   } while(trav.hasMoreOptions)
  *
  * }}}
  */
private[ecspec] class ProbabilityTraverser
    extends TrackingTraverser(1.0)(_ * _) {

  /**
    * Probability the end up in the current state. Should be between 0.0 and 1.0 where zero means impossible and one a 100 % certainty.
    */
  def probability: Double = state
}
