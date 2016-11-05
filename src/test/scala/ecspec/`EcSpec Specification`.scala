package ecspec

import org.specs2.Specification

object `EcSpec Specification` extends Specification {

  def is =
    s2"""

ExSpec helps to find and test race conditions on code using the Scala execution context.
Race conditions can occur when operations have side effects like using `foreach`, `Promise.complete`
or changes in the underlining data source (e.g. database).

The example below could print either "foreach 1: result", "foreach 2: result" or "foreach 2: result",
"foreach 1: result", depending on the scheduler. (The first case is much more likely to occur.)

```
val f = Future { "result" }

f.foreach { x => println(s"foreach 1: $$x") }
f.foreach { x => println(s"foreach 2: $$x") }
```

$pending The EcSpec provide the `everyPossiblePath` method, which allows to test every possible execution path.
The test below would _allways_ fail if it would be somehow possible that the value of `x` becomes something
other than 0, 1, or 2.

```
class ExecutionContextTest extends PropSpec with Matchers with EcSpec {

  property("find all possible solutions") {
    everyPossiblePath { implicit ec =>
      @volatile var x = 0

      Future { x = 1 }
      Future { x = 2 }

      x should (be(0) or be(1) or be(2))
    }
  }
}
```

## General assumptions about the test code

`EcSpec` base upon the assumption that every code block has at most on observable atomic side-effect.
A code block means here:

* a closure provides to an asynchronous method
* split at points where a new task is scheduled
* or the `pass()` method is called (see below for more details about that)

In the example above five code blocks can be found, marked below. The blocks 1, 2, and 4

```
everyPossiblePath { implicit ec =>
  // Block 1
  @volatile var x = 0

  Future { // Block 2
   x = 1
  }
  // Block 3
  Future { // Block 4
    x = 2
  }

  // Block 5
  x should (be(0) or be(1) or be(2))
}
```

If this assumption is violated it's impossible to find all interleavings and errors could remain undetected.
E.g. if the block 4 above would be `x.set(5); x.set(2)` the test would still be green, but during the runtime
the value 5 would be observable. In this case either split to operation in to two or using the `pass()`-method.

## Could Word

With the well known `should` test word, it's possible the that some predicate hold always. Sometimes it's also
valuable to show that something could happen. In the example above this could be the question if it is possible
that the value of `x` is 1.

```
everyPossiblePath { implicit ec =>
  @volatile var x = 0

  Future { x = 1 }
  Future { x = 2 }

  x could be(1)
}
```

## Runtime

Notice that the test block is executed as fast as normal test code but it's executed very often.
Think roughly of 2<sup>n</sup>, where n is the number of code blocks (as defined above.
Our leading example in this introduction requires twelve rounds of execution.

## Atomic actions

Very often multiple pathes are semantically equal.

## Internal structures

${"Traverser" ~/ `Traverser Specification`} - implements the traversation of all possible reachable states

"""

}
