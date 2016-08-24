package actors

import akka.actor._
import models.response._
import play.api.libs.json._

object UserActor {
  def props(out: ActorRef, topic: ActorRef, userId: Long, name: String): Props = Props(new UserActor(out, topic, userId, name))
  trait Factory {
    // Corresponds to the @Assisted parameters defined in the constructor
    def apply(out: ActorRef, topic: ActorRef): Actor
  }

  case class ChangeName(name: String)
}

class UserActor (val out: ActorRef, val topic: ActorRef, val userId: Long, _name: String) extends Actor with JsonFormat {
  var name = _name
  override def receive: Receive = {
    //case value: JsObject =>
    //  topic ! value + ("userId", Json.toJson(userId)) + ("name", Json.toJson(name))
    case value: Test =>
      topic ! View.Message(value.message, userId, name)
    case value: JsValue =>
      // クライアントからの入力をリクエストを表す型に変換
      for {
        envelope <- value.validate[Envelope]
        request <- envelope.`type` match {
          case "test" => envelope.value.validate[Test]
        }
      } yield self ! request
//    case user: User =>
//      out ! Json.toJson(user)
    case UserActor.ChangeName(_name) =>
      name = _name
  }

}
