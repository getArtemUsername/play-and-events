package controllers

import java.util.UUID

import play.api.mvc.Controller
import security.UserAuthAction
import services.AnswerEventProducer

import scala.concurrent.Future

/**
  *
  * AnswerController class
  *
  * @author artem klevakin
  */
class AnswerController(userAuthAction: UserAuthAction, answerEventProducer: AnswerEventProducer) extends Controller {

  case class CreateAnswerData(questionId: UUID, answerText: String)

  case class DeleteAnswerData(questionId: UUID, answerId: UUID)

  case class UpdateAnswerData(questionId: UUID, answerId: UUID, answerText: String)

  case class UpvoteAnswerData(questionId: UUID, answerId: UUID)

  import play.api.data.Form
  import play.api.data.Forms._

  val upvoteAnswerForm = Form {
    mapping(
      "questionId" -> uuid,
      "answerId" -> uuid
    )(UpvoteAnswerData.apply)(UpvoteAnswerData.unapply)
  }

  val createAnswerForm = Form {
    mapping(
      "questionId" -> uuid,
      "answerText" -> nonEmptyText
    )(CreateAnswerData.apply)(CreateAnswerData.unapply)
  }

  val deleteAnswerForm = Form {
    mapping(
      "questionId" -> uuid,
      "answerId" -> uuid
    )(DeleteAnswerData.apply)(DeleteAnswerData.unapply)
  }

  val updateAnswerForm = Form {
    mapping(
      "questionId" -> uuid,
      "answerId" -> uuid,
      "answerText" -> nonEmptyText
    )(UpdateAnswerData.apply)(UpdateAnswerData.unapply)
  }

  def createAnswer() = userAuthAction.async {
    implicit request =>
      createAnswerForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest),
        data => {
          answerEventProducer.createAnswer(data.questionId, data.answerText, request.user.userId).map {
            case Some(_) => InternalServerError
            case None => Ok
          }
        }
      )
  }

  def deleteAnswer() = userAuthAction.async { implicit request =>
    deleteAnswerForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest),
      data => {
        answerEventProducer.deleteAnswer(data.questionId,
          data.answerId, request.user.userId).map {
          case Some(error) => InternalServerError
          case None => Ok
        }
      }
    )
  }

  def updateAnswer() = userAuthAction.async { implicit request =>
    updateAnswerForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest),
      data => {
        answerEventProducer.updateAnswer(data.questionId,
          data.answerId, request.user.userId, data.answerText).map {
          case Some(error) => InternalServerError
          case None => Ok
        }
      }
    )
  }

  def upvoteAnswer() = userAuthAction.async { implicit request =>
    upvoteAnswerForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest),
      data => {
        answerEventProducer.upvoteAnswer(data.questionId, data.answerId,
          request.user.userId).map {
          case Some(error) => InternalServerError
          case None => Ok
        }
      }
    )
  }

  def downvoteAnswer() = userAuthAction.async { implicit request =>
    upvoteAnswerForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest),
      data => {
        answerEventProducer.downvoteAnswer(data.questionId, data.answerId,
          request.user.userId).map {
          case Some(error) => InternalServerError
          case None => Ok
        }
      }
    )
  }
}


