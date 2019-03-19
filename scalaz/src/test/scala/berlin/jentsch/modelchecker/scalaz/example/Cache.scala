package berlin.jentsch.modelchecker.scalaz.example

import berlin.jentsch.modelchecker.scalaz.Interpreter
import org.scalatest.{FlatSpec, Matchers}
import scalaz.zio._
import scalaz.zio.syntax._

object Cache {
  def apply[I, O](cached: I => UIO[O]): ZIO[Any, Nothing, I => UIO[O]] =
    for {
      c <- RefM.make(Map.empty[I, O])
    } yield { i: I =>
      c.modify {
        case inCache if inCache.isDefinedAt(i) =>
          (inCache(i), inCache).succeed
        case notInCache =>
          cached(i).map { o: O =>
            (o, notInCache + (i -> o))
          }
      }
    }
}

class CacheSpec extends FlatSpec with Matchers {
  behavior of "a cache"

  private val runCache: UIO[Int] = for {
    counter <- Ref.make(0)
    cache <- Cache[Int, Int](i => counter.update(_ + 1).const(i))
    f1 <- cache(2).fork
    _ <- cache(2).fork
    _ <- cache(1).fork
    _ <- cache(1).fork
    _ <- f1.join // TODO: Why is this line necessary?
    r <- counter.get
  } yield r

  it should "rewritable by the interpreter" in {
    Interpreter.terminatesAlwaysSuccessfully(runCache) should be(Set(1, 2))
  }
}
