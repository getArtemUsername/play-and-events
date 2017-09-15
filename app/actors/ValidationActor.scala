package actors

import java.util.UUID

import actors.ValidationActor.{RefreshStateCommand, ValidationEventRequest}
import akka.actor.{Actor, Props}
import com.appliedscala.events._
import scalikejdbc._

import scala.util.{Failure, Success, Try}

/**
  * ValidationActor class
  */
class ValidationActor extends Actor {

  override def receive: Receive = {
    case ValidationEventRequest(event) =>
      sender() ! processSingleEvent(event, skipValidation = false)
    case RefreshStateCommand(events, fromScratch) =>
      val resetResult = resetState(fromScratch)
      resetResult match {
        case None => processEvents(events, skipValidation = true)
        case _ => resetResult
      }
    case _ => sender() ! Some("Unknown message type!")
  }

  //Tags
  private def validateTagCreated(tagText: String, userId: UUID): Option[String] = {

    validateUser(userId) {
      val maybeExistingT = Try {
        NamedDB('validation).readOnly { implicit session =>
          sql"SELECT tag_id FROM tags WHERE tag_text = $tagText".
            map(_.string("tag_id")).headOption().apply()
        }
      }
      maybeExistingT match {
        case Success(Some(_)) => Some("The tag already exists!")
        case Success(None) => None
        case _ => Some("Validation state exception!")
      }
    }

  }

  private def updateTagCreated(tagId: UUID, tagText: String): Option[String] = {
    invokeUpdate {
      NamedDB('validation).localTx {
        implicit session =>
          sql"INSERT INTO tags(tag_id, tag_text) VALUES ($tagId, $tagText)".update().apply()
      }
    }
  }

  private def validateTagDeleted(tagId: UUID, userId: UUID): Option[String] = {
    validateUser(userId) {
      val maybeExistingTag = NamedDB('validation).readOnly { implicit session =>
        sql"SELECT tag_id FROM tags tn WHERE tag_id = ${tagId}".
          map(_.string("tag_id")).headOption().apply()
      }
      val maybeDependentQuestions = NamedDB('validation).readOnly { implicit session =>
        sql"SELECT question_id FROM tag_question tq WHERE tag_id = ${tagId}".
          map(_.string("question_id")).list().apply()
      }
      (maybeExistingTag, maybeDependentQuestions) match {
        case (None, _) => Some("This tag doesn't exist!")
        case (_, head :: tail) => Some("There are questions that depend on this tag!")
        case (_, Nil) => None
      }
    }
  }

  private def updateTagDeleted(tagId: UUID): Option[String] = {
    invokeUpdate {
      NamedDB('validation).localTx { implicit session =>
        sql"DELETE FROM tags WHERE tag_id = ${tagId}".update().apply()
      }
    }
  }

  //User
  private def validateUser(userId: UUID)(block: => Option[String]): Option[String] = {
    val isActivatedT = isActivated(userId)
    isActivatedT match {
      case Success(false) => Some("The user is not activated!")
      case Failure(_) => Some("Validation state exception!")
      case Success(true) => block
    }
  }

  private def isActivated(userId: UUID): Try[Boolean] = {
    Try {
      NamedDB('validation).readOnly {
        implicit session =>
          sql"SELECT user_id FROM active_users WHERE user_id = $userId"
            .map(_.string("user_id")).headOption().apply().isDefined
      }
    }
  }

  private def validateUserActivated(userId: UUID): Option[String] = {
    val isActivatedT = isActivated(userId)
    isActivatedT match {
      case Success(true) => Some("The user is already activated!")
      case Failure(_) => Some("Validation state exception!")
      case Success(false) => None
    }
  }

  private def validateUserDeactivated(userId: UUID): Option[String] = {
    val isActivatedT = isActivated(userId)
    isActivatedT match {
      case Success(false) => Some("The user is already deactivated!")
      case Failure(_) => Some("Validation state exception!")
      case Success(true) => None
    }
  }

  private def updateUserActivated(userId: UUID): Option[String] = {
    invokeUpdate {
      NamedDB('validation).localTx {
        implicit session =>
          sql"INSERT INTO active_users(user_id) VALUES ($userId)"
            .update().apply()
      }
    }
  }

  private def updateUserDeactivated(userId: UUID): Option[String] = {
    invokeUpdate {
      NamedDB('validation).localTx {
        implicit session =>
          sql"DELETE FROM active_users WHERE user_id = $userId"
            .update().apply()
      }
    }
  }

  //Questions
  private def validateQuestionDeleted(questionId: UUID, userId: UUID):
  Option[String] = {
    validateUser(userId) {
      val maybeQuestionOwnerT = Try {
        NamedDB('validation).readOnly { implicit session =>
          sql"SELECT user_id FROM question_user WHERE question_id = $questionId".
            map(_.string("user_id")).headOption().apply()
        }
      }
      maybeQuestionOwnerT match {
        case Success(None) => Some("The question doesn't exist!")
        case Success(Some(questionOwner)) =>
          if (questionOwner != userId.toString) {
            Some("This user has no rights to delete this question!")
          } else {
            None
          }
        case _ => Some("Validation state exception!")
      }
    }
  }

  private def updateQuestionDeleted(id: UUID): Option[String] = {
    invokeUpdate {
      NamedDB('validation).localTx { implicit session =>
        sql"DELETE FROM question_user WHERE question_id = $id".update().apply()
      }
    }
  }

  private def validateQuestionCreated(questionId: UUID, userId: UUID, tags: Seq[UUID]): Option[String] = {
    validateUser(userId) {
      val existingTagsT = Try {
        NamedDB('validation).localTx { implicit session =>
          implicit val binderFactory: ParameterBinderFactory[UUID] = ParameterBinderFactory {
            value => (stmt, idx) => stmt.setObject(idx, value)
          }
          val tagIdsSql = SQLSyntax.in(sqls"tag_id", tags)
          sql"SELECT * FROM tags WHERE $tagIdsSql".map(_.string("tag_id")).list().apply().length
        }
      }
      existingTagsT match {
        case Failure(th) => Some("Validation state exception!")
        case Success(num) if num == tags.length => None
        case _ => Some("Some tags referenced by the question do not exist!")
      }
    }
  }

  private def updateQuestionCreated(questionId: UUID, userId: UUID, tags: Seq[UUID]): Option[String] = {
    invokeUpdate {
      NamedDB('validation).localTx { implicit session =>
        sql"INSERT INTO question_user(question_id, user_id) VALUES(${questionId}, ${userId})".update().apply()
        tags.foreach { tagId =>
          sql"INSERT INTO tag_question(tag_id, question_id) VALUES(${tagId}, ${questionId})".update().apply()
        }
      }
    }
  }

  //Answers

  private def validateAnswerCreated(answerId: UUID, userId: UUID, questionId: UUID): Option[String] = {
    validateUser(userId) {
      val resultT = Try {
        NamedDB('validation).readOnly { implicit session =>
          val questionExists =
            sql"SELECT * FROM question_user WHERE question_id = ${questionId}".map(_.string("question_id"))
              .headOption().apply().isDefined
          val answerExists =
            sql"SELECT * FROM answer_user WHERE answer_id = ${answerId}".map(_.string("answer_id")).headOption()
              .apply().isDefined
          val alreadyWritten =
            sql"""SELECT user_id FROM answer_user au INNER JOIN question_answer qa ON au.answer_id = qa.answer_id 
                 WHERE question_id = ${questionId} AND user_id = ${userId}""".map(_.string("user_id")).headOption()
              .apply().isDefined
          (questionExists, answerExists, alreadyWritten)
        }
      }
      resultT match {
        case Success((false, _, _)) => Some("This question doesn't exist!")
        case Success((_, _, true)) => Some("Users can only give one answer to the question!")
        case Success((_, true, _)) => Some("This answer already exists!")
        case Success((true, _, _)) => None
        case Failure(_) => Some("Validation state exception!")
      }
    }
  }

  private def updateAnswerCreated(answerId: UUID, userId: UUID, questionId: UUID): Option[String] = {
    invokeUpdate {
      NamedDB('validation).localTx {
        implicit session =>
          sql"""INSERT INTO answer_user(answer_id, user_id) VALUES(${answerId}, ${userId})"""
            .update().apply()
          sql"""INSERT INTO question_answer(question_id, answer_id) VALUES(${questionId}, ${answerId})"""
            .update().apply()
      }
    }
  }

  private def validateAnswerUpvoted(answerId: UUID, userId: UUID, questionId: UUID): Option[String] = {
    val UserIdStr = userId.toString
    val resultT = Try {
      NamedDB('validation).readOnly { implicit session =>
        val questionExists =
          sql"select * from question_user where question_id = ${questionId}".
            map(_.string("question_id")).headOption().apply().isDefined
        val answerAuthor =
          sql"select user_id from answer_user where answer_id = ${answerId}".
            map(_.string("user_id")).headOption().apply()
        val alreadyUpvoted =
          sql"""select upvoted_by_user_id from answer_upvoter where answer_id = ${answerId} and upvoted_by_user_id = ${userId}""".
            map(_.string("upvoted_by_user_id")).headOption().apply().isDefined
        (questionExists, answerAuthor, alreadyUpvoted)
      }
    }
    resultT match {
      case Success((false, _, _)) => Some("This question doesn't exist!")
      case Success((_, None, _)) => Some("This answer doesn't exist!")
      case Success((_, Some(UserIdStr), _)) => Some("Users cannot like their own answers!")
      case Success((_, Some(_), true)) => Some("Users cannot like answers more than once!")
      case Success((_, Some(_), false)) => None
      case _ => Some("Validation state exception!")
    }
  }

  private def updateAnswerUpvoted(answerId: UUID, userId: UUID): Option[String] = {
    invokeUpdate {
      NamedDB('validation).localTx { implicit session =>
        sql"insert into answer_upvoter(answer_id, upvoted_by_user_id) values(${answerId}, ${userId})".update().apply()
      }
    }
  }

  private def validateAnswerDownvoted(answerId: UUID, userId: UUID, questionId: UUID): Option[String] = {
    val resultT = Try {
      NamedDB('validation).readOnly { implicit session =>
        val questionExists =
          sql"select * from question_user where question_id = ${questionId}".
            map(_.string("question_id")).headOption().apply().isDefined
        val alreadyUpvoted =
          sql"""select upvoted_by_user_id from answer_upvoter where answer_id = ${answerId} and upvoted_by_user_id = ${userId}""".
            map(_.string("upvoted_by_user_id")).headOption().apply().isDefined
        (questionExists, alreadyUpvoted)
      }
    }
    resultT match {
      case Success((false, _)) => Some("This question doesn't exist!")
      case Success((_, true)) => None
      case Success((_, false)) => Some("Users cannot downvote what they haven't upvoted")
      case _ => Some("Validation state exception!")
    }
  }

  private def updateAnswerDownvoted(answerId: UUID, userId: UUID): Option[String] = {
    invokeUpdate {
      NamedDB('validation).localTx { implicit session =>
        sql"delete from answer_upvoter where answer_id = ${answerId} and upvoted_by_user_id = ${userId}".update().apply()
      }
    }
  }


  private def invokeUpdate(block: => Any): Option[String] = {
    val result = Try {
      block
    }
    result match {
      case Success(_) => None
      case Failure(th) => Some("Validation state exception!")
    }
  }

  private def validateAndUpdate(skipValidation: Boolean)
                               (validateBlock: => Option[String])
                               (updateBlock: => Option[String]): Option[String] = {
    if (skipValidation) {
      updateBlock
    } else {
      val validationResult = validateBlock
      validationResult match {
        case None => updateBlock
        case _ => validationResult
      }
    }
  }

  private def processSingleEvent(event: LogRecord, skipValidation: Boolean): Option[String] = {
    event.action match {
      case UserActivated.actionName =>
        val decoded = event.data.as[UserActivated]
        validateAndUpdate(skipValidation) {
          validateUserActivated(decoded.id)
        } {
          updateUserActivated(decoded.id)
        }
      case UserDeactivated.actionName =>
        val decoded = event.data.as[UserDeactivated]
        validateAndUpdate(skipValidation) {
          validateUserDeactivated(decoded.id)
        } {
          updateUserDeactivated(decoded.id)
        }
      case TagCreated.actionName =>
        val decoded = event.data.as[TagCreated]
        validateAndUpdate(skipValidation) {
          validateTagCreated(decoded.text, decoded.createdBy)
        } {
          updateTagCreated(decoded.id, decoded.text)
        }
      case TagDeleted.actionName =>
        val decoded = event.data.as[TagDeleted]
        validateAndUpdate(skipValidation) {
          validateTagDeleted(decoded.id, decoded.deletedBy)
        } {
          updateTagDeleted(decoded.id)
        }
      case QuestionCreated.actionName =>
        val decoded = event.data.as[QuestionCreated]
        validateAndUpdate(skipValidation) {
          validateQuestionCreated(decoded.questionId,
            decoded.createdBy, decoded.tags)
        } {
          updateQuestionCreated(decoded.questionId,
            decoded.createdBy, decoded.tags)
        }
      case QuestionDeleted.actionName =>
        val decoded = event.data.as[QuestionDeleted]
        validateAndUpdate(skipValidation) {
          validateQuestionDeleted(decoded.questionId, decoded.deletedBy)
        } {
          updateQuestionDeleted(decoded.questionId)
        }
      case AnswerCreated.actionName =>
        val decoded = event.data.as[AnswerCreated]
        validateAndUpdate(skipValidation) {
          validateAnswerCreated(decoded.answerId,
            decoded.createBy, decoded.questionId)
        } {
          updateAnswerCreated(decoded.answerId, decoded.createBy, decoded.questionId)
        }
      case AnswerUpvoted.actionName =>
        val decoded = event.data.as[AnswerUpvoted]
        validateAndUpdate(skipValidation) {
          validateAnswerUpvoted(decoded.answerId,
            decoded.userId, decoded.questionId)
        } {
          updateAnswerUpvoted(decoded.answerId,
            decoded.userId)
        }
      case AnswerDownvoted.actionName =>
        val decoded = event.data.as[AnswerDownvoted]
        validateAndUpdate(skipValidation) {
          validateAnswerDownvoted(decoded.answerId,
            decoded.userId, decoded.questionId)
        } {
          updateAnswerDownvoted(decoded.answerId,
            decoded.userId)
        }
      case _ => Some("Unknown event")
    }
  }

  private def processEvents(events: Seq[LogRecord],
                            skipValidation: Boolean): Option[String] = {
    var lastResult: Option[String] = None
    import scala.util.control.Breaks._
    breakable {
      events.foreach {
        event =>
          lastResult match {
            case None => lastResult = processSingleEvent(event, skipValidation)
            case Some(_) => break()
          }
      }
    }
    lastResult
  }

  private def resetState(fromScratch: Boolean): Option[String] = {
    if (!fromScratch) None
    else invokeUpdate {
      NamedDB('validation).localTx {
        implicit session =>
          sql"DELETE FROM answer_upvoter WHERE 1 > 0".update().apply()
          sql"DELETE FROM tag_question WHERE 1 > 0".update().apply()
          sql"DELETE FROM question_answer WHERE 1 > 0".update.apply()
          sql"DELETE FROM active_users WHERE 1 > 0".update().apply()
          sql"DELETE FROM answer_user WHERE 1 > 0".update().apply()
          sql"DELETE FROM question_user WHERE 1 > 0".update().apply()
          sql"DELETE FROM tags WHERE 1 > 0".update().apply()
      }
    }
  }
}

object ValidationActor {

  case class ValidationEventRequest(event: LogRecord)

  case class RefreshStateCommand(events: Seq[LogRecord], fromScratch: Boolean = true)

  val name = "validation-action"
  val path = s"/user/$name"

  def props() = Props(new ValidationActor)
}
