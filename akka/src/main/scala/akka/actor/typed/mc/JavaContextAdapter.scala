package akka.actor.typed.mc

import java.time.Duration
import java.util
import java.util.concurrent.CompletionStage
import java.util.function.BiFunction
import java.util.{Optional, function}

import akka.actor.typed._
import akka.actor.typed.javadsl.ActorContext

import scala.concurrent.duration._
import scala.language.higherKinds
import scala.reflect.ClassTag

private object JavaContextAdapter {
  implicit private def anyNothingIsAnyVoid[HigherType[_]]
      : HigherType[Nothing] =:= HigherType[Void] = =:=.tpEquals.asInstanceOf

  def apply[T](
      scalaVariant: scaladsl.ActorContext[T]
  ): javadsl.ActorContext[T] =
    new ActorContext[T] {
      private def toFiniteDuration(duration: Duration): FiniteDuration =
        duration.getSeconds.seconds + duration.getNano.nanos

      override def asScala: scaladsl.ActorContext[T] = scalaVariant

      override def getSelf: ActorRef[T] = scalaVariant.self

      override def getSystem: ActorSystem[Void] = scalaVariant.system

      override def getLog: Logger = scalaVariant.log

      override def setLoggerClass(clazz: Class[_]): Unit =
        scalaVariant.setLoggerClass(clazz)

      override def getChildren: java.util.List[ActorRef[Void]] = {
        val list = new util.Vector[ActorRef[Void]]
        scalaVariant.children.foreach(ref => list.add(ref))
        list
      }

      override def getChild(name: String): Optional[ActorRef[Void]] =
        scalaVariant
          .child(name)
          .fold(Optional.empty[ActorRef[Void]]())(ref =>
            Optional.of(ref: ActorRef[Void])
          )

      override def spawnAnonymous[U](behavior: Behavior[U]): ActorRef[U] =
        scalaVariant.spawnAnonymous(behavior)

      override def spawnAnonymous[U](
          behavior: Behavior[U],
          props: Props
      ): ActorRef[U] =
        scalaVariant.spawnAnonymous(behavior, props)

      override def spawn[U](behavior: Behavior[U], name: String): ActorRef[U] =
        scalaVariant.spawn(behavior, name)

      override def spawn[U](
          behavior: Behavior[U],
          name: String,
          props: Props
      ): ActorRef[U] =
        scalaVariant.spawn(behavior, name, props)

      override def stop[U](child: ActorRef[U]): Unit = scalaVariant.stop(child)

      override def watch[U](other: ActorRef[U]): Unit =
        scalaVariant.watch(other)

      override def watchWith[U](other: ActorRef[U], msg: T): Unit =
        scalaVariant.watchWith(other, msg)

      override def unwatch[U](other: ActorRef[U]): Unit =
        scalaVariant.unwatch(other)

      override def setReceiveTimeout(timeout: Duration, msg: T): Unit =
        scalaVariant.setReceiveTimeout(toFiniteDuration(timeout), msg)

      override def cancelReceiveTimeout(): Unit =
        scalaVariant.cancelReceiveTimeout()

      override def scheduleOnce[U](
          delay: Duration,
          target: ActorRef[U],
          msg: U
      ) =
        scalaVariant.scheduleOnce(toFiniteDuration(delay), target, msg)

      override def getExecutionContext = scalaVariant.executionContext

      override def messageAdapter[U](
          messageClass: Class[U],
          f: function.Function[U, T]
      ): ActorRef[U] =
        scalaVariant.messageAdapter(f.apply)(ClassTag(messageClass))

      override def ask[Req, Res](
          resClass: Class[Res],
          target: RecipientRef[Req],
          responseTimeout: Duration,
          createRequest: function.Function[ActorRef[Res], Req],
          applyToResponse: BiFunction[Res, Throwable, T]
      ): Unit = ???

      override def pipeToSelf[Value](
          future: CompletionStage[Value],
          applyToResult: BiFunction[Value, Throwable, T]
      ): Unit = ???

      override def asJava: ActorContext[T] = this
    }
}
