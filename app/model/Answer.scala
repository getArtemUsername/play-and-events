package model

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.Json

/**
  * Answer class
  */
case class Answer(answerId: UUID, questionID: UUID, answerText: String, authorId: UUID, authorFullName: Option[String],
                  upvotes: Seq[UUID], updated: DateTime)

object Answer {
  implicit val writes = Json.writes[Answer]
}
