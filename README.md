# Model checker (Work in progress)

Prototype of three explicit state model checker for Scala-Futures, [ZIO](https://zio.dev) and a subset of [akka typed](https://doc.akka.io/docs/akka/current/typed/index.html).

*Why?* Model checking allows to tests for race conditions and similar problems that arise in concurrent programs.
Something that is notorious difficult with normal tests.
This projects enables the power of model checking for Scala programs.

See the examples in tests:
* [Scala future](https://github.com/Jentsch/modelchecker/tree/master/futures/src/test/scala/berlin/jentsch/modelchecker/futures/example)
* [ZIO](https://github.com/Jentsch/modelchecker/tree/master/zio/src/test/scala/zio/modelchecker/example)
* [akka-typed](https://github.com/Jentsch/modelchecker/tree/master/akka/src/test/scala/berlin/jentsch/modelchecker/akka/example)

## Development

[![Build Status](https://travis-ci.org/Jentsch/modelchecker.svg?branch=master)](https://travis-ci.org/Jentsch/modelchecker)

This [sbt](https://www.scala-sbt.org/) project is separated into four sub projects:
* *futures* contains a model checker for scala Futures and their ExecutionContext
* *zio* contains a model checker for zio
* *akka* contains a model checker for Akka
* *benchmarks* helps to justify performance sensitive decisions
* *jpf* helps to compare this project to [Java Pathfinder](https://github.com/javapathfinder/jpf-core)

### Tests

Use `sbt test` to execute all tests.
Notice that example code snippets from scaladoc with [sbt-example](https://github.com/ThoughtWorksInc/sbt-example) are used to test functionality.
For those tests ScalaTest with Matchers is used.

An example:

```scala
/**
  * @example My little object. 
  * {{{
  *   MyObject.x should be(1)
  * }}}
  */
object MyObject {
  val x = 1
}
```

## Documentation

The main source of information is currently the api-documentation generated by scaladoc and the examples in test.

