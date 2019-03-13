package berlin.jentsch.modelchecker.scalaz.example

import berlin.jentsch.modelchecker.scalaz.Interpreter._
import org.scalatest.{FlatSpec, Matchers}
import scalaz.zio._
import scalaz.zio.duration.durationInt

object Cache {
  def apply[I, O](
      cached: I => UIO[O]): UIO[I => IO[Nothing, O]] =
    for {
      c <- Ref.make(Map.empty[I, Promise[Nothing, O]])
    } yield { i: I =>
      for {
        p <- Promise.make[Nothing, O]
        r <- c.modify {
          case inCache if inCache.isDefinedAt(i) =>
            (inCache(i).await, inCache)
          case notInCache =>
            (p.await <* cached(i).flatMap(p.succeed), notInCache + (i -> p))
        }
        r2 <- r
      } yield r2

    }
}

class CacheSpec extends FlatSpec with Matchers with DefaultRuntime {
  behavior of "a cache"

  private val runCache = for {
    cache <- Cache[Int, Int](IO.succeed)
    _ <- cache(2)
    _ <- cache(2)
  } yield ()

  it should "rewritable by the interpreter" in pendingUntilFixed {
    val counter: UIO[Int] =
      concurrentEffectsCounter(runCache)

    unsafeRun(counter.timeout(5.seconds)) should be('defined)
  }
}
