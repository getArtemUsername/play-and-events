package com.appliedscala.events

import java.util.UUID

import play.api.libs.json.{JsValue, Json, Reads}

/**
  *
  * TagCreated class
  * <p/>
  * Description...
  *
  */
case class TagCreated(id: UUID, text: String, createdBy: UUID) extends EventData {
  override val action: String = TagCreated.actionName
  override val json: JsValue = Json.writes[TagCreated].writes(this)
}

object TagCreated {
  val actionName = "tag-created"
  implicit val reads: Reads[TagCreated] = Json.reads[TagCreated]
}
