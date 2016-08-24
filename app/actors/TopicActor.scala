package actors

import akka.actor.{ActorRef, Terminated}
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import models.response.View
import play.api.libs.json.{JsValue, Json}

object TopicActor {
  case class Subscribe(actorRef: ActorRef)
}

class TopicActor extends ActorSubscriber with View.JsonWriter {
  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  var subscribers: Set[ActorRef] = Set()

  override def receive: Receive = {
    case Terminated(actor) =>
      subscribers = subscribers - actor
    case TopicActor.Subscribe(sub) =>
      context.watch(sub)
      subscribers = subscribers + sub
    case view: View =>
      val json = view match {
        case value: View.Event =>
          Json.obj(("type", "Event"), ("value", Json.toJson(value)))
        case value: View.Message =>
          Json.obj(("type", "Message"), ("value", Json.toJson(value)))
        case value: View.Gravity =>
          Json.obj(("type", "Gravity"), ("value", Json.toJson(value)))
      }
      subscribers.foreach(a => a ! json)
  }
}

