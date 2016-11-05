package actors.modelchecking

import scala.annotation.tailrec

/**
  * Helper trait to traverse every possible code path.
  *
  * Internal usage:
  * {{{
  * reset()
  *
  * do {
  *   if (choose(true :: false :: Nil)) {
  *     println(choose(1 ::  2 :: 3 :: Nil)
  *   } else {
  *     println("Hello " + choose("world" :: "homer" :: Nil))
  *   }
  * } while (chooseNext())
  * }}}
  *
  * Should produce the lines (maybe not in this order):
  * ```
  * 1
  * 2
  * 3
  * Hello world
  * Hello home
  * ```
  *
  */
private[modelchecking] final class PathTraverser {

  private lazy val _counts = new ThreadLocal[Seq[Int]]
  private lazy val _step = new ThreadLocal[Integer]

  def choose[T](choices: Seq[T]): T = {
    step = step + 1
    // Is this a new choice?
    if (!counts.isDefinedAt(step))
      counts = counts :+ (choices.length - 1)

    choices(counts(step))
  }

  private[modelchecking] def reset(): Unit = {
    counts = Nil
    step = -1
  }

  private def counts: Seq[Int] = _counts.get

  private def counts_=(l: Seq[Int]) = _counts.set(l)

  private def step: Int = _step.get

  private def step_=(n: Int) = _step.set(n)

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

  @tailrec
  private def generateNext(choose: Seq[Int]): Seq[Int] = choose match {
    case xs :+ 0 => generateNext(xs)
    case xs :+ x => xs :+ (x - 1)
    case empty => empty
  }
}
