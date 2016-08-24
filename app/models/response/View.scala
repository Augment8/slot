package models.response

import play.api.libs.json.{Json, OWrites, Reads}

sealed trait View {

}

object View {
  case class Gravity(x: Double, y: Double, z: Double, userId: Long, name: String) extends View
  case class Message(message: String, userId: Long, name: String) extends View

  trait JsonReader {
    implicit val testReader: Reads[Message] = Json.reads[Message]
  }
  trait JsonWriter {
    implicit val testWriter: OWrites[Message] = Json.writes[Message]
  }
}