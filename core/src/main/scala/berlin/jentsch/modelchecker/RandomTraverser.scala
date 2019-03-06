package berlin.jentsch.modelchecker

/**
  * This [[Traverser]] is useful when the combinatorial explosion denies the usage of the [[EveryPathTraverser]].
  * It only runs [[rounds]] times and returns random results.
  *
  * @param rounds the number of times the loop will be executed
  * @example Returning 100 random results
  * {{{
  * val traverser = new RandomTraverser(100)
  * var resultCounter = Map.empty[Int, Int].withDefaultValue(0)
  *
  * do {
  *   val digit1 = traverser.choose(0 until 10)
  *   val digit2 = traverser.choose(0 until 10)
  *   val result = digit1 * 10 + digit2
  *
  *   resultCounter += result -> (resultCounter(result) + 1)
  * } while (traverser.hasMoreOptions())
  *
  * // It's very likely that this checks passes
  * assert(resultCounter.values.sum == 100)
  * assert(resultCounter.values.max > 1)
  * assert((0 until 100).toSet != resultCounter.keys)
  *
  * assert(traverser.getCurrentPath.length < 3)
  * }}}
  */
private[modelchecker] final class RandomTraverser(private var rounds: Int)
    extends Traverser {

  import scala.util.Random.nextInt

  private var currentPath: List[Int] = Nil

  override def choose[E](choices: Seq[E]): E = {
    require(choices.nonEmpty, "no choices available")

    val choice = nextInt(choices.length)
    currentPath ::= choice

    choices(choice)
  }

  /**
    * @return returns [[rounds]] times true and afterwards always false
    * @example of limited rounds
    * {{{
    * val traverser: Traverser = new RandomTraverser(4)
    * var executedRounds = 0
    *
    * do {
    *   executedRounds += 1
    *   traverser.choose(Seq(1, 2, 3))
    * } while (traverser.hasMoreOptions)
    *
    * assert(executedRounds == 4)
    * }}}
    */
  override def hasMoreOptions(): Boolean =
    if (rounds > 1) {
      rounds -= 1
      currentPath = Nil
      true
    } else {
      false
    }

  override def getCurrentPath: Seq[Int] = currentPath.reverse
}
