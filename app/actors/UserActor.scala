package actors

import akka.actor._
import play.api.libs.json._

object UserActor {
  def props(out: ActorRef, topic: ActorRef, userId: Long, name: String): Props = Props(new UserActor(out, topic, userId, name))
  trait Factory {
    // Corresponds to the @Assisted parameters defined in the constructor
    def apply(out: ActorRef, topic: ActorRef): Actor
  }

  case class ChangeName(name: String)
}

class UserActor (val out: ActorRef, val topic: ActorRef, val userId: Long, _name: String) extends Actor {
  var name = _name
  override def receive: Receive = {
    case value: JsObject =>
      topic ! value + ("userId", Json.toJson(userId)) + ("name", Json.toJson(name))
    case value: JsValue =>
      topic ! value
    case UserActor.ChangeName(_name) =>
      name = _name
  }

}
