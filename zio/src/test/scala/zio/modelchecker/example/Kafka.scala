package zio.modelchecker.example

import zio.ZIO.foreach
import zio.modelchecker.NonDeterministic
import zio.modelchecker.NonDeterministicSpec._
import zio.test.Assertion._
import zio.test._
import zio.{Ref, UIO, URIO}

object Kafka {

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
      suite("with kafka")(
        testSometimes("messages can be consumed in serial order") {
          for {
            k <- Kafka.kafka[Int]
            _ <- foreach(1 to 2)(k.offer(partition = 1))
            _ <- foreach(3 to 4)(k.offer(partition = 2))
            r <- foreach(1 to 4)(_ => k.pull)
          } yield assert(r, equalTo(1 :: 2 :: 3 :: 4 :: Nil))
        },
        testAlways(
          "messages in same topic will are always consumed in serial order"
        ) {
          for {
            kafka <- Kafka.kafka[Int]
            _ <- kafka.offer(1)(1)
            _ <- kafka.offer(1)(2)
            first <- kafka.pull
          } yield assert(first, equalTo(1))
        },
        testSometimes("messages can arrive twice") {
          for {
            kafka <- Kafka.kafka[Int]
            _ <- kafka.offer(1)(1)
            _ <- kafka.offer(1)(2)
            first <- kafka.pull
            second <- kafka.pull
          } yield assert(first, equalTo(1)) && assert(second, equalTo(1))
        },
        testSometimes("messages can arrive many times") {
          for {
            kafka <- Kafka.kafka[Int]
            _ <- kafka.offer(1)(1)
            _ <- kafka.offer(1)(2)
            _ <- kafka.offer(1)(2)
            third <- kafka.pull *> kafka.pull *> kafka.pull
          } yield assert(third, equalTo(1))
        }
      )
    )
