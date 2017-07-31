package services

import java.util.concurrent.TimeUnit

import actors.{EventStreamActor, InMemoryReadActor}
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.pattern.ask
import akka.util.Timeout
import com.appliedscala.events.LogRecord
import dao.Neo4JReadDao
import model.ServerSentMessage
import play.api.Configuration

/**
  * TagEventConsumer class
  */
class TagEventConsumer(readService: ReadService, actorSystem: ActorSystem, 
                       configuration: Configuration, materializer: Materializer,
                       neo4JReadDao: Neo4JReadDao) {
  val topicName = "tags"
  val serviceKafkaConsumer = new ServiceKafkaConsumer(Set(topicName), "read", materializer, actorSystem, 
    configuration, handleEvent)
  
  def handleEvent(event: String): Unit = {
    val maybeLogRecord = LogRecord.decode(event)
    maybeLogRecord.foreach(adjustReadState)
  }
  
  private def adjustReadState(logRecord: LogRecord): Unit = {
    neo4JReadDao.handleEvent(logRecord)
    val tagsT = neo4JReadDao.getAllTags
    tagsT.foreach {
      tags => 
        val update = ServerSentMessage.create("tags", tags)
        val eaActor = actorSystem.actorSelection(EventStreamActor.pathPattern)
        eaActor ! EventStreamActor.DataUpdated(update.json)
    }
  }
}
