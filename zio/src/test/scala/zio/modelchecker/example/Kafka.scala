package zio.modelchecker.example

import zio.ZIO.foreach
import zio.modelchecker.NonDeterministic
import zio.modelchecker.NonDeterministic.{doAnyOf, oneOf}
import zio.modelchecker.NonDeterministicSpec._
import zio.test.Assertion._
import zio.test._
import zio.{Ref, UIO, URIO}

object Kafka {

  /**  simple model of how kafka queues behaves with only one consumer */
  def kafka[P, V]: UIO[Kafka[P, V]] =
    for {
      store <- Ref.make(Map.empty[P, List[V]].withDefaultValue(List.empty))
    } yield new Kafka[P, V] {
      override def offer(key: P)(value: V): UIO[Unit] =
        store.update(m => m.updated(key, m(key) :+ value)).unit

      override def pull: URIO[NonDeterministic, V] =
        for {
          partitions <- store.get
          nonEmptyPartition <- oneOf(
            partitions.keys.filter(k => partitions(k).nonEmpty).toSeq
          )
          result :: rest = partitions(nonEmptyPartition)
          _ <- doAnyOf(
            store.update(_.updated(nonEmptyPartition, rest)),
            UIO.unit // failed update
          )
        } yield result
    }

  trait Kafka[P, V] {
    def offer(partition: P)(i: V): UIO[Unit]
    def pull: URIO[NonDeterministic, V]
  }
}

object KafkaSpec
    extends DefaultRunnableSpec(
      suite("with kafka")(
        testSometimes("messages can be consumed in serial order") {
          for {
            k <- Kafka.kafka[Int, Int]
            _ <- foreach(1 to 2)(k.offer(partition = 1))
            _ <- foreach(3 to 4)(k.offer(partition = 2))
            r <- foreach(1 to 4)(_ => k.pull)
          } yield assert(r, equalTo(1 :: 2 :: 3 :: 4 :: Nil))
        },
        testAlways(
          "messages in same topic will are always consumed in serial order"
        ) {
          for {
            kafka <- Kafka.kafka[Int, Int]
            _ <- kafka.offer(1)(1)
            _ <- kafka.offer(1)(2)
            first <- kafka.pull
          } yield assert(first, equalTo(1))
        },
        testSometimes("messages can arrive twice") {
          for {
            kafka <- Kafka.kafka[Int, Int]
            _ <- kafka.offer(1)(1)
            _ <- kafka.offer(1)(2)
            first <- kafka.pull
            second <- kafka.pull
          } yield assert(first, equalTo(1)) && assert(second, equalTo(1))
        },
        testSometimes("messages can arrive many times") {
          for {
            kafka <- Kafka.kafka[Int, Int]
            _ <- kafka.offer(1)(1)
            _ <- kafka.offer(1)(2)
            _ <- kafka.offer(1)(2)
            third <- kafka.pull *> kafka.pull *> kafka.pull
          } yield assert(third, equalTo(1))
        }
      )
    )
