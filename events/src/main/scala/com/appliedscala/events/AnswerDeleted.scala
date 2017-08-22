package com.appliedscala.events

import java.util.UUID

import play.api.libs.json.{JsValue, Json, Reads}

/**
  * AnswerDeleted class
  * <p/>
  */
case class AnswerDeleted(answerId: UUID, questionId: UUID, deletedBy: UUID) 
  extends EventData {
  override def action: String = AnswerDeleted.actionName
  override def json: JsValue = Json.writes[AnswerDeleted].writes(this)
}

object AnswerDeleted  {
  val actionName = "answer-deleted"
  implicit val reads = Json.reads[AnswerDeleted]
}
