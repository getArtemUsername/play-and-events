package services

import java.util.UUID
import java.util.concurrent.TimeUnit

import actors.InMemoryReadActor
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import com.appliedscala.events.{EventData, LogRecord, TagCreated, TagDeleted}
import dao.LogDao
import model.Tag
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  * TagEventProducer class
  * <p/>
  * Description...
  *
  * @author artem klevakin
  */
class TagEventProducer(actorSystem: ActorSystem, logDao: LogDao, readService: ReadService) {

  private def createLogRecord(eventData: EventData): LogRecord = {
    LogRecord(UUID.randomUUID(), eventData.action, eventData.json, DateTime.now())
  }
  
  def createTag(text: String, createdBy: UUID): Future[Seq[Tag]] = {
    val tagId = UUID.randomUUID()
    val event = TagCreated(tagId, text, createdBy)
    val record = createLogRecord(event)
    
    logDao.insertLogRecord(record) match {
      case Failure(th) => Future.failed(th)
      case Success(_) => adjustReadState(record)
    }
  }

  def deleteTag(tagId: UUID, deletedBy: UUID): Future[Seq[Tag]] = {
    val event = TagDeleted(tagId, deletedBy)
    val record = createLogRecord(event)
    logDao.insertLogRecord(record) match {
      case Failure(th) => Future.failed(th)
      case Success(_) => adjustReadState(record)
    }
  }
  
  private def adjustReadState(logRecord: LogRecord): Future[Seq[Tag]] = {
    implicit val timeout = Timeout(5, TimeUnit.MINUTES)
    val actor = actorSystem.actorSelection(InMemoryReadActor.path)
    (actor ? InMemoryReadActor.ProcessEvent(logRecord)).flatMap {
      _ => readService.getTags
    }
  }

}
