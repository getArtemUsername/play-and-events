package services

import java.time.DateTimeException
import java.util.UUID

import akka.actor.ActorSystem
import com.appliedscala.events.{AnswerCreated, LogRecord}
import org.joda.time.DateTime
import play.api.Configuration

import scala.concurrent.Future

/**
  *
  * AnswerEventProducer class
  * <p/>
  * Description...
  */
class AnswerEventProducer(actorSystem: ActorSystem,
                          configuration: Configuration,
                          eventValidator: EventValidator) {
  val kafkaProducer = new ServiceKafkaProducer("answers", actorSystem, configuration)
  
  def createAnswer(questionId: UUID, answerText: String, createdBy: UUID): Future[Option[String]] = {
    val answerId = UUID.randomUUID()
    val created = DateTime.now()
    val event = AnswerCreated(answerId, answerText, questionId, createdBy, created)
    val record = LogRecord.fromEvent(event)
    eventValidator.validateAndSend(createdBy, record, kafkaProducer)
  }
}
