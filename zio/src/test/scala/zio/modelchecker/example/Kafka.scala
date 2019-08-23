package zio.modelchecker.example

import org.scalatest.{FlatSpec, Matchers}
import zio.{Ref, UIO, ZIO}
import zio.ZIO.foreach
import zio.modelchecker.{Interpreter, NonDeterministic}

object Kafka {
  def apply(): ZIO[NonDeterministic, Nothing, Seq[Int]] =
    for {
      k <- kafka
      _ <- foreach(1 to 2)(k.offer(partition = 1))
      _ <- foreach(3 to 4)(k.offer(partition = 2))
      r <- foreach(1 to 4)(_ => k.pull)
    } yield r

  /**  simple model how kafka queues behaves */
  def kafka: UIO[Kafka] =
    for {
      q1 <- Ref.make(List.empty[Int])
      q2 <- Ref.make(List.empty[Int])
    } yield new Kafka {
      override def offer(partition: Int)(i: Int): UIO[Unit] =
        (if (partition % 2 == 0) q1 else q2).update(_ :+ i).unit

      override def pull: ZIO[NonDeterministic, Nothing, Int] =
        NonDeterministic.doOneOf(
          q1.get.map(_.nonEmpty) -> q1.modify { qs =>
            (qs.head, qs.tail)
          },
          q1.get.map(_.nonEmpty) -> q1.modify { qs =>
            (qs.head, qs)
          },
          q2.get.map(_.nonEmpty) -> q2.modify { qs =>
            (qs.head, qs.tail)
          },
          q2.get.map(_.nonEmpty) -> q2.modify { qs =>
            (qs.head, qs)
          }
        )
    }

  trait Kafka {
    def offer(partition: Int)(i: Int): UIO[Unit]
    def pull: ZIO[NonDeterministic, Nothing, Int]
  }
}

class KafkaSpec extends FlatSpec with Matchers {
  behavior of "Read kafka messages"

  private val results =
    Interpreter.everyPath.terminatesAlwaysSuccessfully(Kafka())

  they can "come in perfect order" in {
    results should contain(List(1, 2, 3, 4))
  }

  they should "keep order in the same partition" in {
    results.foreach { seq =>
      if (seq.indexOf(2) >= 0)
        assert(seq.indexOf(1) <= seq.indexOf(2))
    }
  }

  they can "produce the same value twice" in {
    results should contain(List(1, 1, 2, 3))
  }

  they can "produce the same value many times" in {
    results should contain(List(3, 1, 1, 1))
  }
}
