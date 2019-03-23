package berlin.jentsch.modelchecker.example
import berlin.jentsch.modelchecker.{EveryPathTraverser, Traverser}
import org.scalatest.FlatSpec

class FerrymanSpec extends FlatSpec {

  it should "be possible to carry over all parts" in {
    val traverser: Traverser = new EveryPathTraverser
    var pathsCount = 0
    var everDone = false
    var wolfBeforeCabbage = false
    var cabbageBeforeWolf = false
    do {
      var wolf, goat, cabbage, ferryman: Boolean = false
      /** Upper bound found by experimentation, actually 7 is enough */
      var steps = 8

      def done: Boolean = wolf && goat && cabbage && ferryman

      /** Either when one thing gets eaten or when no more steps available */
      def failed: Boolean =
        (wolf == goat && wolf != ferryman) ||
          (goat == cabbage && goat != ferryman) ||
          (steps <= 0)

      def allActions: List[(Boolean, () => Unit)] =
        (wolf == ferryman) -> (() => { wolf = !wolf; ferryman = !ferryman }) ::
          (goat == ferryman) -> (() => { goat = !goat; ferryman = !ferryman }) ::
          (cabbage == ferryman) -> (() => {
          cabbage = !cabbage; ferryman = !ferryman
        }) ::
          true -> (() => { ferryman = !ferryman }) ::
          Nil

      def possibleActions: List[() => Unit] = allActions.collect {
        case (true, action) => action
      }

      while (!done && !failed) {
        traverser.choose(possibleActions)()
        steps -= 1
      }

      wolfBeforeCabbage |= wolf && !goat & !cabbage
      cabbageBeforeWolf |= ! wolf && !goat & cabbage

      if (!everDone && done) {
        info("First found path: " ++ traverser.getCurrentPath.toString)
      }
      everDone |= done
      pathsCount += 1
    } while (traverser.hasMoreOptions())

    info("Paths: " ++ pathsCount.toString)
    assert(everDone)
    assert(wolfBeforeCabbage)
    assert(cabbageBeforeWolf)
  }
}
