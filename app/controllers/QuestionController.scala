package controllers

import java.util.UUID

import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results._
import security.UserAuthAction
import services.{QuestionEventProducer, ReadService}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * QuestionController class
  */
class QuestionController(questionEventProducer: QuestionEventProducer,
                         userAuthAction: UserAuthAction,
                         readService: ReadService) {

  case class CreateQuestionData(title: String, details: Option[String], tags: Seq[UUID])

  case class DeleteQuestionData(id: UUID)

  import play.api.data.Forms._
  import scala.concurrent.ExecutionContext.Implicits.global

  val createQuestionForm = Form {
    mapping(
      "title" -> nonEmptyText,
      "details" -> optional(text),
      "tags" -> seq(uuid)
    )(CreateQuestionData.apply)(CreateQuestionData.unapply)
  }

  val deleteQuestionForm = Form {
    mapping(
      "id" -> uuid
    )(DeleteQuestionData.apply)(DeleteQuestionData.unapply)
  }

  def createQuestion() = userAuthAction.async {
    implicit request =>
      createQuestionForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest),
        data => {
          val resultF = questionEventProducer.createQuestion(data.title, data.details, data.tags, request.user.userId)
          resultF.map {
            case Some(error) => InternalServerError
            case None => Ok
          }
        }
      )

  }

  def deleteQuestion() = userAuthAction {
    implicit request =>
      deleteQuestionForm.bindFromRequest.fold(
        formWithErros => BadRequest,
        data => {
          questionEventProducer.deleteQuestion(data.id, request.user.userId)
          Ok
        }
      )

  }

  def getQuestions() = Action {
    implicit request =>
      val questionsT = readService.getAllQuestions
      questionsT match {
        case Failure(_) => InternalServerError
        case Success(questions) => Ok(Json.toJson(questions))
      }
  }
}
