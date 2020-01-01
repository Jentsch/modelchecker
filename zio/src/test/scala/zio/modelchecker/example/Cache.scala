package zio.modelchecker.example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio._
import zio.syntax._
import zio.modelchecker.Interpreter

object Cache {
  def apply[I, O](cached: I => UIO[O]): UIO[I => UIO[O]] =
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

class CacheSpec extends AnyFlatSpec with Matchers {
  behavior of "a cache"

  private val runCache: UIO[Int] = for {
    invocationCount <- Ref.make(0)
    cache <- Cache[Int, Int](i => invocationCount.update(_ + 1).as(i))
    f1 <- cache(2).fork
    _ <- cache(2).fork
    _ <- cache(1).fork
    _ <- cache(1).fork
    _ <- f1.join // On why this line is necessary see the LostFiberSpec
    r <- invocationCount.get
  } yield r

  it should "rewritable by the interpreter" in {
    Interpreter.terminatesAlwaysSuccessfully(runCache) should be(Set(1, 2))
  }
}
