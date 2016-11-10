import org.specs2.Specification

object index extends Specification {
  title("Modelchecking")

  def is =
    s2"""

[Visit on GitHub](https://github.com/Jentsch/modelchecker)
[API](latest/api/index.html)

## EcSpec

Run tests with a modified execution context.

More details about the EcSpec

${"Traverser" ~/ ecspec.`EcSpec Specification`} - implements the traversation of all possible reachable states

"""
}
