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
}

class UserActor (val out: ActorRef, val topic: ActorRef, val userId: Long, _name: String) extends Actor with JsonFormat {
  var name = _name
  override def receive: Receive = {
    case user: User =>
      user match {
        case value: Test =>
          topic ! View.Message(value.message, userId, name)
        case value: PressButton =>
          topic ! View.Event(value.value, userId, name)
        case value: ChangeName =>
          name = value.name
        case value: Gravity =>
          topic ! View.Gravity(value.x, value.y, value.z, userId, name)
      }
    case value: JsValue =>
      // クライアントからの入力をリクエストを表す型に変換
      for {
        envelope <- value.validate[Envelope]
        request <- envelope.`type` match {
          case "test" => envelope.value.validate[Test]
          case "ChangeName" => envelope.value.validate[ChangeName]
          case "PressButton" => envelope.value.validate[PressButton]
          case "Gravity" => envelope.value.validate[Gravity]
        }
      } yield self ! request
  }

}
