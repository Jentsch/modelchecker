package test

object NativePhilosophers {

  val n = 3

  type Stick = Object

  val chopsticks: Array[Stick] =
    Array.fill(n)(new Object)

  class philosopher(firstStick: Stick, secondStick: Stick) extends Thread {
    override def run(): Unit = {
      firstStick.synchronized {
        secondStick.synchronized {
          // EAT
        }
      }
    }
  }

  def runDeadLock: Unit =
    0 until n foreach { i =>
      new philosopher(chopsticks(i), chopsticks((i + 1) % n)).start()
    }

  def runOk: Unit =
    0 until n foreach {
      case 0 => new philosopher(chopsticks(n - 1), chopsticks(0)).start()
      case i => new philosopher(chopsticks(i), chopsticks((i + 1) % n)).start()
    }

  def main(args: Array[String]): Unit = {
    runOk
  }
}
