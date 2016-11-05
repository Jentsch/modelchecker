package examples.cache

import scala.concurrent.Future

class AsyncCache[I, O](
    f: I => Future[O],
    prediction: => Traversable[I]
) extends (I => Future[O]) {
  override def apply(key: I): Future[O] = f(key)

  def refresh(): Unit = {

  }
}

object AsyncCache {
  def apply[I, O](f: I => Future[O], prediction: => Traversable[I]) =
    new AsyncCache[I, O](f, prediction)
}
