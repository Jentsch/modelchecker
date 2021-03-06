package berlin.jentsch.modelchecker

/**
  * @example usage
  * {{{
  *   val trav = new EveryPathTraverser
  *   var out = Set.empty[Int]
  *
  *   do {
  *     val c: Int = trav.choose(Seq(1, 2, 3))
  *     if (c >= 2) {
  *       out += c * trav.choose(Seq(1, 2))
  *     } else {
  *       out += c
  *     }
  *   } while(trav.hasMoreOptions())
  *
  *   out should be(Set(1, 2, 4, 3, 6))
  * }}}
  *
  * Should produce the output 1, 2, 3 (not separated by commas but in multiple lines)
  */
final class EveryPathTraverser extends Traverser {

  /** Path in the tree of choices */
  private[this] var path = Array.fill(50)(0)
  private[this] var pathLength = 0
  private[this] var currentDepth = 0

  /**
    * Choose 'randomly' one option.
    *
    * @return an element of all choices
    * @throws IllegalArgumentException, if no choice is offered
    */
  override def choose[E](choices: Seq[E]): E = {
    if (needToChoose(choices)) {
      val choiceIndex = if (currentDepth >= pathLength) {
        if (pathLength == path.length) {
          path = java.util.Arrays.copyOf(path, pathLength * 4)
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
    } else {
      choices.head
    }
  }

  /**
    * Returns the current path. Can be used to recreate a single run with the traverse
    *
    * @return current path
    * @example usage of getCurrentPath and [[SinglePath]]
    *
    * {{{
    *   val trav = new EveryPathTraverser
    *   val seeking = 12
    *   var foundPath: Seq[Int] = Seq.empty
    *
    *   def calculation(walk: Traverser): Int = {
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
    *   } while(trav.hasMoreOptions())
    *
    *   foundPath should not(be(empty))
    *
    *   calculation(new SinglePath(foundPath)) should be(seeking)
    * }}}
    */
  override def getCurrentPath: Seq[Int] = path.toSeq.take(currentDepth)

  override def getCurrentPathLength: Int = currentDepth

  /**
    * @return `false` if no next round could be generated
    * @example returns false if no choice was made
    * {{{
    *   val trav = new EveryPathTraverser
    *
    *   trav.hasMoreOptions() should be(false)
    * }}}
    */
  def hasMoreOptions(): Boolean = {
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

      true
    } else {
      false
    }
  }

  override def reset(): Unit = {
    path = Array.fill(50)(0)
    pathLength = 0
    currentDepth = 0
  }
}
