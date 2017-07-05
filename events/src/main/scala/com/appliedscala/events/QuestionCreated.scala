package com.appliedscala.events

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Reads}

/**
  * QuestionCreated class
  * */
case class QuestionCreated(title: String, details: Option[String],
                      tags: Seq[UUID], questionId: UUID, createdBy: UUID,
                      created: DateTime)  extends EventData {
  override def action: String = QuestionCreated.actionName
  override def json: JsValue = Json.writes[QuestionCreated].writes(this) 
}

object QuestionCreated {
  val actionName = "question-created"
  implicit val reads: Reads[QuestionCreated] = Json.reads[QuestionCreated]
}
