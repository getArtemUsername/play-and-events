package model

import play.api.libs.json.{JsValue, Json, OWrites, Writes}

/**
  *
  * ServerSentMessage class
  * <p/>
  * Description...
  *
  * @author artem klevakin
  */
case class ServerSentMessage(updateType: String, updateData: JsValue) {
  def json: JsValue = Json.toJson(this)(ServerSentMessage.writes)
}

object ServerSentMessage {
  val writes: Writes[ServerSentMessage] = Json.writes[ServerSentMessage]
  def create[T](updateType: String, updateData: T)(implicit encoder: Writes[T]) =
    ServerSentMessage(updateType, encoder.writes(updateData))
}
