package ecspec

import scala.collection.mutable

/**
  * {{{
  *   val trav = new Traverser
  *
  *   do {
  *     val c: Int = trav.choose(Seq(1, 2, 3))
  *     println(c)
  *   } while(trav.hasMoreOptions)
  * }}}
  *
  * Should produce the output 1, 2, 3 (not separated by commas but in multiple lines)
  */
private[ecspec] class Traverser {

  /** Path in the tree of choices */
  private val path = mutable.ListBuffer.empty[Int]
  private var currentDepth = 0

  /**
    * Remove and return 'randomly' one element of the given mutable sequence.
    */
  def removeOne[E](choices: mutable.Buffer[E]): E = {
    assert(choices.nonEmpty)
    val choiceIndex = choose(choices.indices)
    val choice = choices(choiceIndex)
    choices.remove(choiceIndex)

    choice
  }

  /**
    * Choose 'randomly' one option
    */
  def choose[E](choices: Seq[E]): E = {
    assert(choices.nonEmpty)

    val choiceIndex = path.lift(currentDepth).getOrElse {
      val max = choices.length - 1
      path.append(max)
      max
    }

    currentDepth += 1

    choices(choiceIndex)
  }

  /**
    * @return false if no next round could be generated
    */
  def hasMoreOptions: Boolean = {
    currentDepth = 0
    decrementPath()
  }

  /**
    * @return true if the path was decremented
    */
  private def decrementPath(): Boolean = {
    var depth = path.length - 1

    while (depth >= 0 && path(depth) == 0) {
      path.remove(depth)
      depth -= 1
    }

    if (depth >= 0) {
      path(depth) -= 1

      return true
    } else {
      return false
    }
  }
}
