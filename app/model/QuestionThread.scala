package model

import play.api.libs.json.Json

/**
  *
  * QuestionThread class
  * <p/>
  */
case class QuestionThread(question: Question, answers: Seq[Answer]) 

object QuestionThread {
  implicit val writes = Json.writes[QuestionThread]
}
