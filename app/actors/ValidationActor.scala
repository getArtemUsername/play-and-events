package actors

import java.util.UUID

import akka.actor.{Actor, Props}
import com.appliedscala.events.{LogRecord, TagCreated}
import scalikejdbc._

import scala.util.{Failure, Success, Try}

/**
  * ValidationActor class
  */
class ValidationActor extends Actor {
  override def receive: Receive = ???

  private def validateTagCreated(tagText: String): Option[String] = {

    val maybeExistingT = Try {
      NamedDB('validation).readOnly {
        implicit session =>
          sql"SELECT tag_id FROM tags WHERE tag_text = $tagText".
            map(_.string("tag_id")).headOption().apply()
      }
    }
    maybeExistingT match {
      case Success(Some(_)) => Some("The tag already exists")
      case Success(None) => None
      case _ => Some("Validation state exception")
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

  private def invokeUpdate(block: => Any): Option[String] = {
    val result = Try {
      block
    }
    result match {
      case Success(_) => None
      case Failure(th) => Some("Validation state exception!")
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

  private def withValidatedUser(userId: UUID)(block: => Option[String]): Option[String] = {
    val isActivatedT = isActivated(userId)
    isActivatedT match {
      case Success(false) => Some("The user is not activated!")
      case Failure(_) => Some("Validation state exception")
      case Success(true) => block
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
      case TagCreated.actionName =>
        val decoded = event.data.as[TagCreated]
        validateAndUpdate(skipValidation) {
          validateTagCreated(decoded.text)
        } {
          updateTagCreated(decoded.id, decoded.text)
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
            case Some(_) => None
          }
      }
    }
    lastResult
  }
}

object ValidationActor {

  case class ValidationEventRequest(event: LogRecord)

  case class RefreshStateCommand(events: Seq[LogRecord], fromScratch: Boolean = true)

  val name = "validation-action"
  val path = s"/user/$name"

  def props() = Props(new ValidationActor)
}
