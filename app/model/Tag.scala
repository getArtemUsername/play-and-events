package model

import java.util.UUID

import play.api.libs.json.Json

/**
  *
  * Tag class
  * <p/>
  * Description...
  *
  */
case class Tag(id: UUID, text: String)

object Tag {
  implicit val writes = Json.writes[Tag]
}
