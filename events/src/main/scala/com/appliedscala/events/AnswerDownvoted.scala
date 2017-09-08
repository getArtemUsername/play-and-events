package com.appliedscala.events

import java.util.UUID

import play.api.libs.json.Json

/**
  *
  * AnswerDownvoted class
  * <p/>
  */
case class AnswerDownvoted(questionId: UUID, answerId: UUID, userId: UUID) extends EventData {
  override def action = "answer-downvoted"

  override def json = Json.writes[AnswerDownvoted].writes(this)
}

object AnswerDownvoted {
  val actionName = "answer-downvoted"
  implicit val reads = Json.reads[AnswerDownvoted]
}
