package ecspec

import scala.collection.mutable

/**
  * @example
  * {{{
  *   val trav = new Traverser
  *   var out = Set.empty[Int]
  *
  *   do {
  *     val c: Int = trav.choose(Seq(1, 2, 3))
  *     out += c
  *   } while(trav.hasMoreOptions)
  *
  *   out should be(Set(1, 2, 3))
  * }}}
  *
  * Should produce the output 1, 2, 3 (not separated by commas but in multiple lines)
  */
private[ecspec] class Traverser {

  /** Path in the tree of choices */
  private val path = Array.fill(100)(-1)
  private var pathLength = 0
  private var currentDepth = 0

  /**
    * Remove and return 'randomly' one element of the given mutable sequence.
    *
    * @example
    * {{{
    *   val trav = new Traverser
    *
    *   do {
    *     val original = List(1, 2, 3)
    *     val choices = original.to[collection.mutable.Buffer]
    *
    *     val chosen = trav.removeOne(choices)
    *
    *     choices should have length 2
    *
    *     original diff choices should contain only(chosen)
    *   } while(trav.hasMoreOptions)
    * }}}
    *
    * @example empty buffers are an invalid argument for `removeOne`
    * {{{
    *   val trav = new Traverser
    *
    *   a[IllegalArgumentException] should be thrownBy trav.removeOne(collection.mutable.Buffer.empty)
    * }}}
    */
  def removeOne[E](choices: mutable.Buffer[E]): E = {
    val choiceIndex = choose(choices.indices)
    val choice = choices(choiceIndex)
    choices.remove(choiceIndex)

    choice
  }

  /**
    * Choose 'randomly' one option
    */
  def choose[E](choices: Seq[E]): E = {
    require(choices.nonEmpty, "no choices available")

    val choiceIndex = if (currentDepth >= pathLength) {
      val max = choices.length - 1
      path(currentDepth) = max
      pathLength += 1
      max
    } else {
      path(currentDepth)
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
    * @return true if the [[path]] was decremented
    */
  private def decrementPath(): Boolean = {
    var depth = pathLength - 1

    while (depth >= 0 && path(depth) == 0) {
      pathLength -= 1
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
