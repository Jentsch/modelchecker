package berlin.jentsch.modelchecker.futures.example

import ecspec.EcSpec
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class AsyncCacheSpec extends FlatSpec with EcSpec with Matchers {

  behavior of "Async Cache"

  it should "return same result" in everyInterleaving { implicit ec =>
    val f = { i: Int =>
      Future { i.toString }
    }
    val cache = AsyncCache(f)

    cache(1) will be("1")
    cache(1) will be("1")
  }

  it should "cache" in everyInterleaving { implicit ec =>
    var seenRequest = Set.empty[Int]

    val f = { i: Int =>
      seenRequest should not contain i
      seenRequest += i
      Future {
        i.toString
      }
    }
    val cache = AsyncCache(f)

    atomic {
      ec.execute(() => cache(1) will be("1"))
      ec.execute(() => cache(1) will be("1"))
    }
  }

  it should "not store failures" in everyInterleaving { implicit ec =>
    var response: Try[String] =
      Failure(new Exception)

    val f = { _: Int =>
      Future { response.get }
    }

    val cache = AsyncCache(f)

    cache(1).onComplete { r =>
      r should be a 'failure
      response = Success("Foo")

      cache(1) will be("Foo")
    }

  }

  it should "valuate on request" in everyInterleaving { implicit ec =>
    val f = { _: Int =>
      Future.firstCompletedOf(
        List(
          Future { "Foo" },
          Future { throw new Exception("") }
        ))
    }

    val cache = AsyncCache(f)

    cache(1).onComplete { r =>
      r could be(a[Failure[_]])
      r could be(a[Success[_]])
    }
  }
}
