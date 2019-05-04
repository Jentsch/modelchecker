package berlin.jentsch.modelchecker

/**
  * @see [[EveryPathTraverser.getCurrentPath]] for how to use this class
  */
private[modelchecker] final class SinglePath(path: Seq[Int]) extends Traverser {

  private var index: Int = 0

  override def choose[E](choices: Seq[E]): E = {
    if (needToChoose(choices)) {
      val result = path.lift(index).fold(choices.last)(choices)
      index += 1
      result
    } else {
      choices.head
    }
  }

  override def hasMoreOptions(): Boolean = false

  /**
    * Returns the path that was given to this single path instance.
    *
    * @example it equals the given path
    * {{{
    *   val path = new SinglePath(Seq(1, 2, 3))
    *
    *   path.getCurrentPath shouldBe Seq(1, 2, 3)
    *
    *   path.choose(Seq(true, false))
    *
    *   path.getCurrentPath shouldBe Seq(1, 2, 3)
    * }}}
    */
  override def getCurrentPath: Seq[Int] = path

  override def getCurrentPathLength: Int = path.length
}
