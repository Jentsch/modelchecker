package actors

trait ActorSystem {
  type Message = String
  type Behaviour = Function[Message, Unit]

  private val effects = new ThreadLocal[Effects]
  private var initial = List.empty[Actor]

  protected def initialActors = initial

  /** Use this Behaviour to become dead */
  val dead: Behaviour = { (any: Any) => ()}

  /**
   * Actor
   *
   * Have object itself has to be stateless. The only way to store state is within Behaviour by the 'init' value and the
   * 'become' method.
   */
  trait Actor {
    /**
     * Sends a message to the actor.
     * The sender will be only transmitted iff it is part of the message.
     */
    def !(message: Message): Unit =
      effects.set(effects.get.copy(messages = effects.get.messages :+ (message -> this)))

    val init: Behaviour

    final protected def become(b: Behaviour): Unit =
      effects.set(effects.get.copy(behaviour = b))

    final protected def create(actor: Actor): Unit =
      effects.set(effects.get.copy(actors = effects.get.actors :+ actor))

    /**
     * Override this method create helper actors, send initial messages, or update the initial behaviour.
     */
    def creation(): Unit = { }

    final protected[actors] def processCreation: Effects =
      effectsOf(init) {
        creation
      }

    final protected def self: this.type = this

    final protected[actors] def process(behaviour: Behaviour, message: Message): Effects =
      effectsOf(behaviour){() =>
        behaviour(message)
      }

    private[this] def effectsOf(behaviour: Behaviour)(action: () => Unit): Effects = {
      effects.set(Effects(this, behaviour, Nil, Nil))
      action()
      val result = effects.get
      effects.remove()
      result
    }

    // Register this new actor to system
    if (effects.get == null) {
      initial = initial :+ this
    }
  }

  protected case class Effects(
                                of: Actor, behaviour: Behaviour,
                                messages: List[(Message, Actor)],
                                actors: List[Actor]) {
    def amend(that: Effects): Effects = {
      // If this requirement stays it could be expressed by types
      require(this.of == that.of)
      copy(messages = this.messages ::: that.messages, actors = this.actors ::: that.actors)
    }
  }

}
