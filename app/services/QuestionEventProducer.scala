package services

import java.util.UUID

import akka.actor.ActorSystem
import com.appliedscala.events.{LogRecord, QuestionCreated, QuestionDeleted}
import org.joda.time.DateTime
import play.api.Configuration

import scala.concurrent.Future

/**
  * QuestionEventProducer class
  */
class QuestionEventProducer(actorSystem: ActorSystem,
                            confuration: Configuration, eventValidator: EventValidator) {

  val kafkaProducer = new ServiceKafkaProducer("questions", actorSystem, confuration)

  def createQuestion(title: String, details: Option[String], tags: Seq[UUID], createdBy: UUID): Future[Option[String]] = {
    val questionId = UUID.randomUUID()
    val created = DateTime.now()
    val event = QuestionCreated(title, details, tags, questionId, createdBy, created)
    val record = LogRecord.fromEvent(event)
    eventValidator.validateAndSend(createdBy, record, kafkaProducer)
  }

  def deleteQuestion(questionId: UUID, deletedBy: UUID): Unit = {
    val event = QuestionDeleted(questionId, deletedBy)
    val record = LogRecord.fromEvent(event)
    eventValidator.validateAndSend(deletedBy, record, kafkaProducer)
  }
}
