package com.appliedscala.events

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Reads}

/**
  * AnswerCreated class
  * <p/>
  */
case class AnswerCreated(answerId: UUID, answerText: String, 
                         questionId: UUID, createBy:UUID, created: DateTime) extends EventData {
  override def action: String = AnswerCreated.actionName
  override def json: JsValue = Json.writes[AnswerCreated].writes(this)
}

object AnswerCreated {
  val actionName = "answer-created"
  implicit val reads = Json.reads[AnswerCreated]
}
