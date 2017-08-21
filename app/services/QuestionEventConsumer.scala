package services

import actors.EventStreamActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.impl.StreamSupervisor.Materialize
import com.appliedscala.events.LogRecord
import dao.Neo4JReadDao
import model.ServerSentMessage
import play.api.Configuration

/**
  * QuestionEventConsumer class
  */
class QuestionEventConsumer(neo4JReadDao: Neo4JReadDao, actorSystem: ActorSystem,
                            configuration: Configuration, materializer: Materializer,
                            readService: ReadService) {

  val topicName = "questions"
  val serviceKafkaConsumer = new ServiceKafkaConsumer(Set(topicName), "read", materializer, 
    actorSystem, configuration, handleEvent)
  
  private def handleEvent(event: String): Unit = {
    val maybeLogRecord = LogRecord.decode(event)
    maybeLogRecord.foreach(adjustReadState)
  }

  private def adjustReadState(logRecord: LogRecord): Unit = {
    neo4JReadDao handleEvent logRecord
    val questionsT = readService.getAllQuestions
    questionsT.foreach {
      questions=>
        val update = ServerSentMessage.create("questions", questions)
        val esActor = actorSystem.actorSelection(EventStreamActor.pathPattern)
        esActor ! EventStreamActor.DataUpdated(update.json)
    }
  }
  
}
