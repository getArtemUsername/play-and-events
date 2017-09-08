package com.appliedscala.events

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}

/**
  *
  * AnswerUpdated class
  * <p/>
  */
case class AnswerUpdated(answerId: UUID, answerText: String, questionId: UUID, updatedBy: UUID, updated: DateTime) 
  extends EventData {
  override def action = AnswerUpdated.actionName
  override def json: JsObject = Json.writes[AnswerUpdated].writes(this)
}

object AnswerUpdated {
  val actionName = "answer-updated"
  implicit val reads = Json.reads[AnswerUpdated]
}
