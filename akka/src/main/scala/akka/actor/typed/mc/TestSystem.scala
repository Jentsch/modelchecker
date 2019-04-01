package akka.actor.typed.mc

import java.time.Duration
import java.util
import java.util.concurrent.{CompletionStage, ThreadFactory}
import java.util.function.BiFunction
import java.util.{Optional, function}

import akka.actor.typed._
import akka.actor.typed.internal.InternalRecipientRef
import akka.actor.{ActorPath, ActorRefProvider, Cancellable, DynamicAccess, Scheduler}
import akka.util.Timeout
import berlin.jentsch.modelchecker.akka.root

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag
import scala.util.Try

class TestSystem[R](init: Behavior[R]) {

  case class ActorState[T](msgs: Seq[T], behavior: Behavior[T])

  val currentState: mutable.Map[ActorPath, ActorState[_]] =
    mutable.Map(root -> ActorState(Seq.empty, init))

  class MActorContext[T](path: ActorPath)
      extends TypedActorContext[T]
      with javadsl.ActorContext[T]
      with scaladsl.ActorContext[T] {
    override def asJava: javadsl.ActorContext[T] = this
    override def asScala: scaladsl.ActorContext[T] = this

    override def getSelf: ActorRef[T] = MActorRef(path)
    override def getSystem: ActorSystem[Void] = MActorSystem.asInstanceOf
    override def getLog: Logger = log
    override def getChildren: util.List[ActorRef[Void]] = ???
    override def getChild(name: String): Optional[ActorRef[Void]] =
      child(name) match {
        case Some(value) => Optional.of(value.asInstanceOf)
        case None        => Optional.empty()
      }
    override def spawnAnonymous[U](behavior: Behavior[U]): ActorRef[U] = ???
    override def spawn[U](behavior: Behavior[U], name: String): ActorRef[U] = {
      val childPath = path / name
      currentState += (childPath -> ActorState(Seq.empty, behavior))

      MActorRef(childPath)
    }
    override def setReceiveTimeout(timeout: Duration, msg: T): Unit = ???
    override def scheduleOnce[U](
        delay: Duration,
        target: ActorRef[U],
        msg: U
    ): Cancellable = ???
    override def getExecutionContext: ExecutionContextExecutor = ???
    override def messageAdapter[U](
        messageClass: Class[U],
        f: function.Function[U, T]
    ): ActorRef[U] =
      ???
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
    override def self: ActorRef[T] = getSelf
    override def system: ActorSystem[Nothing] = getSystem
    override def log: Logger = ???
    override def setLoggerClass(clazz: Class[_]): Unit = ???
    override def children: Iterable[ActorRef[Nothing]] = ???
    override def child(name: String): Option[ActorRef[Nothing]] = {
      val childPath = path / name

      if (currentState.isDefinedAt(childPath))
        Some(MActorRef(childPath))
      else
        None
    }
    override def spawnAnonymous[U](
        behavior: Behavior[U],
        props: Props
    ): ActorRef[U] = ???
    override def spawn[U](
        behavior: Behavior[U],
        name: String,
        props: Props
    ): ActorRef[U] = ???
    override def stop[U](child: ActorRef[U]): Unit = ???
    override def watch[U](other: ActorRef[U]): Unit =
      ???
    override def watchWith[U](other: ActorRef[U], msg: T): Unit = ???
    override def unwatch[U](other: ActorRef[U]): Unit =
      ???
    override def setReceiveTimeout(timeout: FiniteDuration, msg: T): Unit = ???
    override def cancelReceiveTimeout(): Unit = ???
    override def scheduleOnce[U](
        delay: FiniteDuration,
        target: ActorRef[U],
        msg: U
    ): Cancellable = ???
    override implicit def executionContext: ExecutionContextExecutor = ???
    override def spawnMessageAdapter[U](f: U => T, name: String) = ???
    override def spawnMessageAdapter[U](f: U => T) = ???
    override def messageAdapter[U](f: U => T)(
        implicit evidence$1: ClassTag[U]
    ): ActorRef[U] = ???
    override def ask[Req, Res](
        target: RecipientRef[Req]
    )(createRequest: ActorRef[Res] => Req)(
        mapResponse: Try[Res] => T
    )(implicit responseTimeout: Timeout, classTag: ClassTag[Res]): Unit = ???
    override def pipeToSelf[Value](future: Future[Value])(
        mapResult: Try[Value] => T
    ): Unit = ???
  }

  trait MActorRef[T] extends ActorRef[T] with InternalRecipientRef[T] {
    override val path: ActorPath

    override def isTerminated: Boolean =
      currentState.get(path).exists(_.behavior == Behavior.stopped)
    override def tell(msg: T): Unit = {
      ???
    }
    override def narrow[U <: T]: ActorRef[U] = this.asInstanceOf
    override def unsafeUpcast[U >: T]: ActorRef[U] = this.asInstanceOf
    override def provider: ActorRefProvider = ???
    override def compareTo(o: ActorRef[_]): Int = this.path compareTo o.path
  }

  def MActorRef[T](_path: ActorPath): MActorRef[T] = new MActorRef[T] {
    override val path = _path
  }

  object MActorSystem
      extends ActorSystem[R]
      with InternalRecipientRef[R]
      with MActorRef[R] {
    override val path: ActorPath = root
    override def name: String = "TestActorSystem"
    override def settings: Settings = ???
    override def logConfiguration(): Unit = ???
    override def log: Logger = ???
    override def startTime: Long = ???
    override def uptime: Long = ???
    override def threadFactory: ThreadFactory = ???
    override def dynamicAccess: DynamicAccess = ???
    override def scheduler: Scheduler = ???
    override def dispatchers: Dispatchers = ???
    override implicit def executionContext: ExecutionContextExecutor = ???
    override def terminate(): Future[Terminated] = ???
    override def whenTerminated: Future[Terminated] = ???
    override def getWhenTerminated: CompletionStage[Terminated] = ???
    override def deadLetters[U]: ActorRef[U] = ???
    override def printTree: String = currentState.toString()
    override def systemActorOf[U](
        behavior: Behavior[U],
        name: String,
        props: Props
    )(implicit timeout: Timeout): Future[ActorRef[U]] =
      ???
    override def registerExtension[T <: Extension](ext: ExtensionId[T]): T =
      sys.error("Extensions are not supported")
    override def extension[T <: Extension](ext: ExtensionId[T]): T =
      sys.error("Extensions are not supported")
    override def hasExtension(ext: ExtensionId[_ <: Extension]): Boolean = false
  }
}
