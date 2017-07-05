package com.appliedscala.events

import java.util.UUID

import play.api.libs.json.{JsValue, Json, Reads}

/**
  * QuestionDeleted class
  */
case class QuestionDeleted(questoinId: UUID, deletedBy: UUID) extends EventData {
  override def action: String = QuestionDeleted.actionName
  override def json: JsValue = Json.writes[QuestionDeleted].writes(this)
}

object QuestionDeleted {
  val actionName = "question-deleted"
  implicit val reads: Reads[QuestionDeleted] = Json.reads[QuestionDeleted]
}
