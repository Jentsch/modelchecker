package actors.modelchecking

/**
 * Usage:
 * {{{
 *
 * reset()
 *
 * do {
 *   if (choose(true, false)) {
 *     println(1, 2, 3)
 *   } else {
 *     println("Hello " + choose("world", "homer"))
 *   }
 * } while (chooseNext())
 * }}}
 *
 * Should produce the lines:
 * ```1, 2, 3, Hello world, Hello home```
 *
 * To concurrent threads doesn't influence each other.
 */
trait Choose {

  private lazy val _counts = new ThreadLocal[Seq[Int]]
  private def counts: Seq[Int] = _counts.get
  private def counts_=(l: Seq[Int]) = _counts.set(l)
  private lazy val _step = new ThreadLocal[Integer]
  private def step: Int = _step.get
  private def step_=(n: Int) = _step.set(n)

  private[modelchecking] def reset(): Unit = {
    counts = Nil
    step = -1
  }

  /**
   * Checks the if more choices are possible and prepare the next.
   *
   * Returns false directly after ```reset()```
   */
  private[modelchecking] def chooseNext(): Boolean = {
    counts = generateNext(counts)
    step = -1

    counts != Nil
  }

  private def generateNext(choose: Seq[Int]): Seq[Int] = choose match {
    case xs :+ 0 => generateNext(xs)
    case xs :+ x => xs :+ (x - 1)
    case empty   => empty
  }

  /**
   * Choose none deterministic a given option
   */
  protected def choose[T](head: T, tail: T*): T = {
    step = step + 1
    // Is this a new choice?
    if (!counts.isDefinedAt(step))
      counts = counts :+ tail.length

    if (counts(step) == 0)
      head
    else
      tail(counts(step) - 1)
  }
}