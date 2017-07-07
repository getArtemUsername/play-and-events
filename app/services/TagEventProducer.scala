package services

import java.util.UUID
import java.util.concurrent.TimeUnit

import actors.{EventStreamActor, InMemoryReadActor}
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import com.appliedscala.events.{EventData, LogRecord, TagCreated, TagDeleted}
import dao.LogDao
import model.Tag
import org.joda.time.DateTime
import play.api.Configuration

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * TagEventProducer class
  */
class TagEventProducer(actorSystem: ActorSystem,
                       validationService: ValidationService,
                       eventValidator: EventValidator,
                       configuration: Configuration) {

  val kafkaProducer = new ServiceKafkaProducer("tags", actorSystem, configuration)

  def createTag(text: String, createdBy: UUID): Unit = {
    val tagId = UUID.randomUUID()
    val event = TagCreated(tagId, text, createdBy)
    val record = createLogRecord(event)
    eventValidator.validatiteAndSend(createdBy, record, kafkaProducer)
  }

  def deleteTag(tagId: UUID, deletedBy: UUID): Unit = {
    val event = TagDeleted(tagId, deletedBy)
    val record = createLogRecord(event)
    kafkaProducer.send(record.encode)
    eventValidator.validatiteAndSend(deletedBy, record, kafkaProducer)
  }

  private def createLogRecord(eventData: EventData): LogRecord = {
    LogRecord(UUID.randomUUID(), eventData.action, eventData.json, DateTime.now())
  }
}
