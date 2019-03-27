package test

import scala.concurrent.{ExecutionContext, Future, Promise}

object FuturePhilosophers {
  implicit val ec: ExecutionContext = new ExecutionContext {
    override def execute(runnable: Runnable): Unit =
      new Thread {
        override def run(): Unit = runnable.run()
      }.start()
    override def reportFailure(cause: Throwable): Unit = ???
  }

  val n = 3

  case class Chopstick(var f: Future[Unit]) {
    def getAndSet(next: Future[Unit]): Future[Unit] = {
      val prev = f
      f = next
      prev
    }
  }

  val chopsticks: Array[Chopstick] =
    Array.fill(n)(Chopstick(Future.successful(())))

  def philosopher(firstStick: Chopstick,
                  secondStick: Chopstick): Future[Unit] = {
    val (firstPromise, secondPromise) = (Promise[Unit], Promise[Unit])

    for {
      // THINK
      _ <- firstStick.getAndSet(firstPromise.future)
      _ <- secondStick.getAndSet(secondPromise.future)
      // EAT
    } yield {
      secondPromise.success(())
      firstPromise.success(())
    }
  }

  def runDeadLock(): Future[_] =
    Future.sequence(List.tabulate(n) { i =>
      philosopher(chopsticks(i), chopsticks((i + 1) % n))
    })

  def runOk(): Future[_] =
    Future.sequence(List.tabulate(n) {
      case 0 => philosopher(chopsticks(n - 1), chopsticks(0))
      case i => philosopher(chopsticks(i), chopsticks((i + 1) % n))
    })

  def mustTerminate(f: Future[_]): Unit = {
    val lock = new Object

    f.foreach { _ =>
      lock.synchronized(lock.notify())
    }

    lock.synchronized(lock.wait())
  }

  def main(args: Array[String]): Unit = {
    mustTerminate(runOk())
  }
}
