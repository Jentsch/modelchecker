package akka.actor.typed.mc

import java.time.Duration
import java.util
import java.util.concurrent.{CompletionStage, ThreadFactory}
import java.util.function.BiFunction
import java.util.{Optional, function}

import akka.Done
import akka.actor.typed.Behavior.DeferredBehavior
import akka.actor.typed._
import akka.actor.typed.internal.BehaviorImpl.ReceiveMessageBehavior
import akka.actor.typed.internal.InternalRecipientRef
import akka.actor.typed.scaladsl.Behaviors.ReceiveImpl
import akka.actor.{ActorPath, ActorRefProvider, Cancellable}
import akka.util.Timeout
import berlin.jentsch.modelchecker.EveryPathTraverser
import berlin.jentsch.modelchecker.akka.{ActorState, NonDeterministic, root}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag
import scala.util.Try

final class TestSystem[R](
    var currentState: Map[ActorPath, ActorState],
    atoms: Seq[Map[ActorPath, ActorState] => Boolean]
) {

  private var currentActor: ActorPath = _
  private val trav = new EveryPathTraverser

  private object NonDet extends NonDeterministic {
    def one[T](actions: (() => T)*): T = {
      trav.choose(actions)()
    }
    def oneOf[T](traversable: Traversable[T]): T = {
      trav.choose(traversable.toSeq)
    }
  }

  def nextStates: Set[Map[ActorPath, ActorState]] = {

    runDeferred()

    var next = Set.empty[Map[ActorPath, ActorState]]
    val startState = currentState

    startState.foreach {
      case (path, ActorState(behavior, messages)) =>
        currentState = startState
        currentActor = path

        def run[T](rec: ExtensibleBehavior[T]): Unit = messages.foreach {
          case (sender, messages) =>
            val message = messages.head
            if (messages.tail == Nil) {
              currentState = modify(startState, path)(
                s => s.copy(messages = s.messages - sender)
              )
            } else {
              currentState = modify(startState, path)(
                s => s.copy(messages = modify(s.messages, sender)(_.tail))
              )
            }

            val stateBefore = currentState
            val ctx = MActorContext[T](path)
            var max = 6

            do {
              currentState = stateBefore
              val result = rec.receive(ctx, message.asInstanceOf[T])

              currentState =
                modify(currentState, path)(_.copy(behavior = result))

              runDeferred()

              next += currentState

              max -= 1
              if (max <= 0)
                throw new RuntimeException("To many")
            } while (trav.hasMoreOptions())
        }

        behavior match {
          case deferred: DeferredBehavior[_] =>
            val result = deferred(MActorContext(path))
            currentState = modify(currentState, path)(_.copy(behavior = result))
            next += currentState

          case rec: ReceiveMessageBehavior[_] =>
            run(rec)

          case rec: ReceiveImpl[_] =>
            run(rec)

          case Behavior.EmptyBehavior | Behavior.StoppedBehavior =>
        }
    }

    next
  }

  def runDeferred(): Unit = {
    val initialAtomsState: Seq[Boolean] = atoms.map(_(currentState))
    var dontRun: Set[ActorPath] = Set.empty
    var limit = 100
    val isCandidate: ((ActorPath, ActorState)) => Boolean = {
      case (path, state) =>
        state.behavior.isInstanceOf[DeferredBehavior[_]] && !dontRun(path)
    }

    while (currentState.exists(isCandidate)) {
      currentState
        .find(isCandidate)
        .foreach {
          case (path, ActorState(b: DeferredBehavior[_], _)) =>
            val oldState = currentState
            currentActor = path
            val result = b(MActorContext(path))
            currentState = modify(currentState, path)(_.copy(behavior = result))
            if (initialAtomsState != atoms.map(_(currentState))) {
              currentState = oldState
              dontRun += path
            }
        }

      limit -= 1
      require(
        limit >= 0,
        "Limit for deferred behaviours reached. Maybe this is a loop?"
      )
    }
  }

  def MActorContext[T](path: ActorPath): TypedActorContext[T] =
    new MActorContext[T](path)

  class MActorContext[T](path: ActorPath)
      extends TypedActorContext[T]
      with javadsl.ActorContext[T]
      with scaladsl.ActorContext[T] {
    override def asJava: javadsl.ActorContext[T] = this
    override def asScala: scaladsl.ActorContext[T] = this

    override def getSelf: ActorRef[T] = MActorRef(path)
    override def getSystem: ActorSystem[Void] = MActorSystem
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

      require(!currentState.isDefinedAt(childPath))

      currentState += (childPath -> ActorState(behavior))

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
    ): ActorRef[U] = {
      require(props == Props.empty)

      spawn(behavior, name)
    }
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

    override private[akka] def onUnhandled(msg: T): Unit = ???

    override private[akka] def currentBehavior: Behavior[T] =
      currentState(path).behavior.asInstanceOf

    override private[akka] def hasTimer: Boolean = ???

    override private[akka] def cancelAllTimers(): Unit = ???
  }

  trait MActorRef[T] extends ActorRef[T] with InternalRecipientRef[T] {
    override val path: ActorPath

    override def isTerminated: Boolean =
      ???

    override def tell(msg: T): Unit = {
      currentState = modify(currentState, path)(
        s =>
          s.copy(messages = modify(s.messages, currentActor) { msgs =>
            assert(
              msgs.size <= 5,
              "To many messages in queue for actor " ++ path.toString
            )
            msgs :+ msg
          })
      )
    }
    override def narrow[U <: T]: ActorRef[U] = this.asInstanceOf
    override def unsafeUpcast[U >: T]: ActorRef[U] = this.asInstanceOf
    override def provider: ActorRefProvider = ???
    override def compareTo(o: ActorRef[_]): Int = this.path compareTo o.path

    override def equals(o: Any): Boolean = o match {
      case that: MActorRef[_] => this.path == that.path
    }

    override def hashCode(): Int = path.hashCode()

    override def toString: String = "Ref(" + path.toStringWithoutAddress + ")"

  }

  def MActorRef[T](_path: ActorPath): MActorRef[T] = new MActorRef[T] {
    override val path = _path
  }

  object MActorSystem
      extends ActorSystem[Void]
      with InternalRecipientRef[Void]
      with MActorRef[Void] {

    import akka.actor.{DynamicAccess, Scheduler}

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
    override def terminate(): Unit = ???
    override def whenTerminated: Future[Done] = ???
    override def getWhenTerminated: CompletionStage[Done] = ???
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
    override def extension[T <: Extension](ext: ExtensionId[T]): T = ext match {
      case NonDeterministic => NonDet: NonDeterministic
      case _                => sys.error("Extensions are not supported")
    }
    override def hasExtension(ext: ExtensionId[_ <: Extension]): Boolean = false
  }

  private def modify[K, V](map: Map[K, V], key: K)(mod: V => V): Map[K, V] =
    try {
      map + (key -> mod(map(key)))
    } catch {
      case _: NoSuchElementException =>
        throw new NoSuchElementException(
          key.toString ++ " not found in " ++ map.toString
        )
    }
}

object TestSystem {
  def apply(
      currentState: Map[ActorPath, ActorState],
      atoms: Set[Map[ActorPath, ActorState] => Boolean]
  ): TestSystem[_] =
    new TestSystem(currentState, atoms.toSeq)
}
