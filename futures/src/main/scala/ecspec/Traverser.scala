package ecspec

import java.util.Arrays

import scala.collection.mutable

/**
  * @example usage
  * {{{
  *   val trav = new Traverser
  *   var out = Set.empty[Int]
  *
  *   do {
  *     val c: Int = trav.choose(Seq(1, 2, 3))
  *     if (c >= 2) {
  *       out += c * trav.choose(Seq(1, 2))
  *     } else {
  *       out += c
  *     }
  *   } while(trav.hasMoreOptions)
  *
  *   out should be(Set(1, 2, 4, 3, 6))
  * }}}
  *
  * Should produce the output 1, 2, 3 (not separated by commas but in multiple lines)
  */
private[ecspec] final class Traverser extends Walker {

  /** Path in the tree of choices */
  private var path = Array.fill(50)(0)
  private var pathLength = 0
  private var currentDepth = 0

  /**
    * Choose 'randomly' one option.
    *
    * @return an element of all choices
    * @throws IllegalArgumentException, if no choice is offered
    */
  override def choose[E](choices: Seq[E]): E = {
    require(choices.nonEmpty, "no choices available")

    val choiceIndex = if (currentDepth >= pathLength) {
      if (pathLength == path.length) {
        path = Arrays.copyOf(path, pathLength * 4)
      }
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
    * Returns the current path. Can be used to recreate a single run with the traverse
    *
    * @return current path
    * @example usage of [[getCurrentPath]] and [[ecspec.SinglePath]]
    * {{{
    *   val trav = new Traverser
    *   val seeking = 12
    *   var foundPath: Seq[Int] = Seq.empty
    *
    *   def calculation(walk: Walker): Int = {
    *     val a: Int = walk.choose(Seq(1, 2, 3))
    *     val b: Int = walk.choose(Seq(4, 2))
    *     a * b
    *   }
    *
    *
    *   do {
    *     if (calculation(trav) == seeking) {
    *       foundPath = trav.getCurrentPath
    *     }
    *   } while(trav.hasMoreOptions)
    *
    *   foundPath should not(be(empty))
    *
    *   calculation(SinglePath(foundPath)) should be(seeking)
    * }}}
    */
  def getCurrentPath: Seq[Int] = path.toSeq.take(currentDepth)

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

private[ecspec] abstract class Walker {

  /**
    * Remove and return 'randomly' one element of the given mutable sequence.
    *
    * @throws IllegalArgumentException, if no choice is offered
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
    * @example empty buffers are an invalid argument for `removeOne`
    * {{{
    *   val trav = new Traverser
    *
    *   an[IllegalArgumentException] should be thrownBy trav.removeOne(collection.mutable.Buffer.empty)
    * }}}
    */
  def removeOne[E](choices: mutable.Buffer[E]): E = {
    val choiceIndex = choose(choices.indices)

    choices.remove(choiceIndex)
  }

  def choose[E](choices: Seq[E]): E

  /**
    * @return false if no next round could be generated
    */
  def hasMoreOptions: Boolean

  /**
    *
    * @return
    */
  def getCurrentPath: Seq[Int]
}

/**
  * @see [[ecspec.Traverser.getCurrentPath]] for how to use this class
  */
private[ecspec] final case class SinglePath(private val path: Seq[Int])
    extends Walker {

  private var index: Int = 0

  override def choose[E](choices: Seq[E]): E = {
    val result = path.lift(index).fold(choices.last)(choices)
    index += 1
    result
  }

  override def hasMoreOptions: Boolean = false

  /**
    * Returns the path that was given to this single path instance.
    *
    * @example it equals the given path
    * {{{
    *   val path = SinglePath(Seq(1, 2, 3))
    *
    *   path.getCurrentPath shouldBe Seq(1, 2, 3)
    *
    *   path.choose(Seq(true, false))
    *
    *   path.getCurrentPath shouldBe Seq(1, 2, 3)
    * }}}
    */
  override def getCurrentPath: Seq[Int] = path
}
