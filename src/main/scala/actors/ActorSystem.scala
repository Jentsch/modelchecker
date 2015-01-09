package actors

trait ActorSystem {
  type Message = String
  type Behaviour = Function[Message, Unit]

  private val effects = new ThreadLocal[Effects]
  private var initial = List.empty[Actor]

  protected def initialActors = initial

  /**
   * Actor
   *
   * Have object itself has to be stateless. The only way to store state is the 'become' method.
   */
  trait Actor {
    val dead = { (any: Any) => ()}

    /**
     * Sends a message to the actor. The sender will be only transmitted iff it part of the message.
     * @param message
     */
    def !(message: Message): Unit =
      effects.set(effects.get.copy(messages = effects.get.messages :+ (message -> this)))

    val init: Behaviour

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
      effects.remove
      result
    }

    final protected def become(b: Behaviour): Unit =
      effects.set(effects.get.copy(behaviour = b))

    final protected def create(actor: Actor): Unit =
      effects.set(effects.get.copy(actors = effects.get.actors :+ actor))

    // Register this new actor to system
    if (effects.get == null) {
      initial = initial :+ this
    }
  }

  protected case class Effects(
                                of: Actor, behaviour: Behaviour,
                                messages: List[(Message, Actor)],
                                actors: List[Actor]) {
    def ammend(that: Effects): Effects = {
      // If this requirement stays it could be expressed by types
      require(this.of == that.of)
      copy(messages = this.messages ::: that.messages, actors = this.actors ::: that.actors)
    }
  }

}
