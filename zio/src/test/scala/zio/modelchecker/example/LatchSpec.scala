package zio.modelchecker.example

import zio.modelchecker.NonDeterministicSpec._
import zio.test._
import zio.test.Assertion.equalTo
import zio.{Promise, ZIO}

object LatchSpec
    extends DefaultRunnableSpec(
      suite("latch example")(
        testAlways(
          "this doesn't block"
        ) {
          for {
            latch <- Promise.make[Nothing, Unit]
            _ <- latch.succeed(()).fork
            fiber <- (latch.await *> ZIO.succeed(1)).fork
            result <- fiber.join
          } yield assert(result, equalTo(1))
        }
      )
    )
