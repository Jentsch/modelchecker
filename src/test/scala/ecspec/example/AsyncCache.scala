package ecspec.example

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

class AsyncCache[Request, Response](f: Request => Future[Response])(
    implicit ec: ExecutionContext)
    extends (Request => Future[Response]) {

  private val store = TrieMap.empty[Request, Promise[Response]]

  override def apply(request: Request): Future[Response] = {
    val myPromise = Promise[Response]
    val response = store.getOrElseUpdate(request, myPromise)

    if (response eq myPromise) {
      f(request).onComplete {
        case Success(s) => myPromise.success(s)
        case Failure(f) =>
          store.remove(request)
          myPromise.failure(f)
      }
    }

    response.future
  }
}

object AsyncCache {
  def apply[Request, Response](f: Request => Future[Response])(
      implicit ec: ExecutionContext): AsyncCache[Request, Response] =
    new AsyncCache(f)
}
