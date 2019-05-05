package berlin.jentsch.modelchecker
import scala.collection.mutable

private[modelchecker] abstract class Traverser {

  /**
    * Remove and return 'randomly' one element of the given mutable sequence.
    *
    * @throws IllegalArgumentException, if no choice is offered
    * @example
    * {{{
    *   val trav = new EveryPathTraverser
    *
    *   do {
    *     val original = List(1, 2, 3)
    *     val choices = original.toBuffer
    *
    *     val chosen = trav.removeOne(choices)
    *
    *     choices should have length 2
    *
    *     original diff choices should contain only(chosen)
    *   } while(trav.hasMoreOptions())
    * }}}
    * @example empty buffers are an invalid argument for `removeOne`
    * {{{
    *   val trav = new EveryPathTraverser
    *
    *   an[IllegalArgumentException] should be thrownBy trav.removeOne(collection.mutable.Buffer.empty)
    * }}}
    */
  def removeOne[E](choices: mutable.Buffer[E]): E = {
    val choiceIndex = choose(choices.indices)

    choices.remove(choiceIndex)
  }

  def choose[E](choices: Seq[E]): E

  protected def needToChoose(choices: Seq[_]): Boolean = {
    require(choices.nonEmpty, "no choices available")

    choices.length > 1
  }

  /**
    * Marks the end of the current round and returns true, if more rounds are needed
    *
    * @return false if no next round could be generated
    */
  def hasMoreOptions(): Boolean

  /**
    *
    * @return
    */
  def getCurrentPath: Seq[Int]

  /**
    * Returns the same as getCurrentPath.length but without constructing the path
    */
  def getCurrentPathLength: Int

}
