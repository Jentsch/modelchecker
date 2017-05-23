package ecspec.example

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object AsyncCache {
  def apply[In, Out](f: In => Future[Out])(
      implicit ec: ExecutionContext): In => Future[Out] = {
    val store = TrieMap.empty[In, Promise[Out]]

    { key =>
      val myPromise = Promise[Out]
      val res = store.getOrElseUpdate(key, myPromise)
      if (res eq myPromise)
        f(key).onComplete {
          case Success(s) => myPromise.success(s)
          case Failure(f) =>
            store.remove(key)
            myPromise.failure(f)
        }

      res.future
    }
  }
}
