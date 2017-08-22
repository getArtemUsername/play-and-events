package com.appliedscala.events

import java.util.UUID

import play.api.libs.json.Json

/**
  *
  * AnswerDownvoted class
  * <p/>
  */
case class AnswerDownvote(questionId: UUID, answerId: UUID, userId: UUID) extends EventData {
  override def action = "answer-downvoted"

  override def json = Json.writes[AnswerDownvote].writes(this)
}

object AnswerDownvote {
  val actionName = "answer-downvoted"
  implicit val reads = Json.reads[AnswerDownvote]
}
