package models.response

import play.api.libs.json.{JsValue, Json, OWrites, Reads}

sealed trait User {

}

case class Envelope(`type`: String,value: JsValue)

case class ChangeName(name: String) extends User
case class PressButton(value: String) extends User
case class SessionId(sessionId: Long) extends User
case class Gravity(x: Double, y: Double, z: Double) extends User
case class Test(message: String) extends User

trait JsonFormat {
  implicit val envelopeJsonRead: Reads[Envelope] = Json.reads[Envelope]

  implicit val sessionIdJsonRead: Reads[SessionId] = Json.reads[SessionId]
  implicit val sessionIdJsonWrite: OWrites[SessionId] = Json.writes[SessionId]
  implicit val gravityJsonRead: Reads[Gravity] = Json.reads[Gravity]
  implicit val testJsonWrite: OWrites[Test] = Json.writes[Test]
  implicit val testJsonRead: Reads[Test] = Json.reads[Test]
  implicit val changeNameJsonRead: Reads[ChangeName] = Json.reads[ChangeName]
  implicit val pressButtonJsonRead: Reads[PressButton] = Json.reads[PressButton]
}
