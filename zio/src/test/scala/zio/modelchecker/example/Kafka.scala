package zio.modelchecker.example

import zio.ZIO.foreach
import zio.modelchecker.Interpreter.everyPath.terminatesAlwaysSuccessfully
import zio.modelchecker.NonDeterministic
import zio.test.Assertion._
import zio.test._
import zio.{Ref, UIO, ZIO}

object Kafka {
  def apply(): ZIO[NonDeterministic, Nothing, List[Int]] =
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

object KafkaSpec
    extends DefaultRunnableSpec(
      suite("Read kafka messages")(
        test("messages can be consumed in serial order") {
          val results: Iterable[List[Int]] =
            terminatesAlwaysSuccessfully(Kafka())
          assert(results, contains(List(1, 2, 3, 4)))
        },
        test("messages in same topic are consumed in serial order") {
          val results: Iterable[List[Int]] =
            terminatesAlwaysSuccessfully(Kafka())
          assert(results, not(contains(List(2, 1, 3, 4))))
        },
        test("messages can arrive twice") {
          val results: Iterable[List[Int]] =
            terminatesAlwaysSuccessfully(Kafka())
          assert(results, contains(List(1, 1, 3, 4)))
        },
        test("messages can arrive many times") {
          val results: Iterable[List[Int]] =
            terminatesAlwaysSuccessfully(Kafka())
          assert(results, contains(List(1, 1, 1, 1)))
        }
      )
    )
