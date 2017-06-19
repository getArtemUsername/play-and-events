package com.appliedscala.events

import java.util.UUID

import play.api.libs.json.{JsValue, Json, Reads}

/**
  *
  * TagDeleted class
  * <p/>
  * Description...
  *
  */
case class TagDeleted(id: UUID, deletedBy: UUID) extends EventData {
  override val action: String = TagDeleted.actionName
  override val json: JsValue = Json.writes[TagDeleted].writes(this)
}

object TagDeleted {
  val actionName: String = "tag-deleted"
  implicit val reads: Reads[TagDeleted] = Json.reads[TagDeleted]
}
