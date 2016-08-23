package actors

import akka.actor.{ActorRef, Terminated}
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import play.api.libs.json.JsValue

object TopicActor {
  case class Subscribe(actorRef: ActorRef)
}

class TopicActor extends ActorSubscriber {
  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  var subscribers: Set[ActorRef] = Set()

  override def receive: Receive = {
    case Terminated(actor) =>
      subscribers = subscribers - actor
    case TopicActor.Subscribe(sub) =>
      context.watch(sub)
      subscribers = subscribers + sub
    case value: JsValue =>
      subscribers.foreach(a => a ! value)
  }
}

