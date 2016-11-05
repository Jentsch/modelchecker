package actors

/**
  * *Contains snippets and ideas*
  *
  * Allows the usage of external systems like web services.
  */
trait IntegratedSystem extends ActorSystem {
  trait ExternalService extends Actor {
    val init: Behaviour = { case _ => ??? }
  }

  class WebService extends ExternalService

  class JsonWebService extends WebService

  class UdpService extends ExternalService
}
