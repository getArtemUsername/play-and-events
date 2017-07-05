package services

import java.util.concurrent.TimeUnit

import actors.{EventStreamActor, InMemoryReadActor}
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.pattern.ask
import akka.util.Timeout
import com.appliedscala.events.LogRecord
import model.ServerSentMessage
import play.api.Configuration

/**
  * TagEventConsumer class
  */
class TagEventConsumer(readService: ReadService, actorSystem: ActorSystem, 
                       configuration: Configuration, materializer: Materializer) {
  val topicName = "tags"
  val serviceKafkaConsumer = new ServiceKafkaConsumer(Set(topicName), "read", materializer, actorSystem, 
    configuration, handleEvent)
  
  def handleEvent(event: String): Unit = {
    val maybeLogRecord = LogRecord.decode(event)
    maybeLogRecord.foreach(adjustReadState)
  }
  
  private def adjustReadState(logRecord: LogRecord): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val timeout = Timeout.apply(5, TimeUnit.SECONDS)
    val imrActor = actorSystem.actorSelection(InMemoryReadActor.path)
    (imrActor ? InMemoryReadActor.ProcessEvent(logRecord)).foreach {
      _ => 
        readService.getTags.foreach {
          tags =>
            val update = ServerSentMessage.create("tags", tags)
            val esActor = actorSystem.actorSelection(EventStreamActor.pathPattern)
            esActor ! EventStreamActor.DataUpdated(update.json)
        }
    }
  }
}
