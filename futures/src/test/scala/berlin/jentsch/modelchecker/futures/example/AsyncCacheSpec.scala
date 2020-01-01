package berlin.jentsch.modelchecker.futures.example
import berlin.jentsch.modelchecker.futures.EcSpec
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class AsyncCacheSpec extends AnyFlatSpec with EcSpec with Matchers {

  behavior of "Async Cache"

  it should "return same result" in everyInterleaving { implicit ec =>
    val f = { i: Int =>
      Future { i.toString }
    }
    val cache = AsyncCache(f)

    cache(1) will be("1")
    cache(1) will be("1")
  }

  it should "cache and invoke the given function twice" in everyInterleaving {
    implicit ec =>
      var seenRequest = Set.empty[Int]

      val f = { item: Int =>
        Future {
          seenRequest should not contain item
          seenRequest += item
          item.toString
        }
      }
      val cache = AsyncCache(f)

      cache(1) will be("1")
      cache(1) will be("1")
  }

  it should "not store failures" in everyInterleaving { implicit ec =>
    var response: Try[String] =
      Failure(new Exception)

    val f = { _: Int =>
      Future { response.get }
    }

    val cache = AsyncCache(f)

    cache(1).onComplete { r =>
      r should be(a[Failure[_]])
      response = Success("Foo")

      cache(1) will be("Foo")
    }

  }
}
