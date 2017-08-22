package com.appliedscala.events

import java.util.UUID

import play.api.libs.json.Json

/**
  *
  * AnswerUpvoted class
  * <p/>
  */
case class AnswerUpvoted(questionId: UUID, answerId: UUID, userId: UUID) extends EventData{
  override def action = AnswerUpvoted.actionName
  override def json = Json.writes[AnswerUpvoted].writes(this) 
}

object AnswerUpvoted {
  val actionName = "answer-upvoted"
  implicit val reads = Json.reads[AnswerUpvoted]
}
