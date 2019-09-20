package zio.modelchecker.example

import zio.ZIO.foreach
import zio.modelchecker.Interpreter.everyPath.terminatesAlwaysSuccessfully
import zio.modelchecker.NonDeterministic
import zio.modelchecker.NonDeterministicSpec._
import zio.test.Assertion._
import zio.test._
import zio.{Ref, UIO, URIO}

object Kafka {
  def apply(): URIO[NonDeterministic, List[Int]] =
    for {
      k <- kafka[Int]
      _ <- foreach(1 to 2)(k.offer(partition = 1))
      _ <- foreach(3 to 4)(k.offer(partition = 2))
      r <- foreach(1 to 4)(_ => k.pull)
    } yield r

  /**  simple model of how kafka queues behaves */
  def kafka[V]: UIO[Kafka[V]] =
    for {
      q1 <- Ref.make(List.empty[V])
      q2 <- Ref.make(List.empty[V])
    } yield new Kafka[V] {
      override def offer(partition: Int)(i: V): UIO[Unit] =
        (if (partition % 2 == 0) q1 else q2).update(_ :+ i).unit

      override def pull: URIO[NonDeterministic, V] =
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

  trait Kafka[V] {
    def offer(partition: Int)(i: V): UIO[Unit]
    def pull: URIO[NonDeterministic, V]
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
        testD("messages in same topic are consumed in serial order") {
          for {
            kafka <- Kafka.kafka[Int]
            _ <- kafka.offer(1)(1)
            _ <- kafka.offer(1)(2)
            _ <- kafka.offer(2)(3)
            first <- kafka.pull
          } yield assert(first, equalTo(1) || equalTo(3))
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
