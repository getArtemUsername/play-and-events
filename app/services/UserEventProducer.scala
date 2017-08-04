package services

import java.util.UUID

import akka.actor.ActorSystem
import com.appliedscala.events.{LogRecord, UserActivated, UserDeactivated}
import play.api.Configuration

/**
  * UserEventProducer class
  */
class UserEventProducer(actorSystem: ActorSystem, configuration: Configuration,
                        eventValidator: EventValidator) {
  
  val kafkaProducer = new ServiceKafkaProducer("users", actorSystem, configuration)
  
  def activateUser(userId: UUID): Unit = {
    val event = UserActivated(userId)
    val record = LogRecord.fromEvent(event)
    eventValidator.validateAndSend(userId, record, kafkaProducer)
  }
  
  def deactivateUser(userId: UUID): Unit = {
    val event = UserDeactivated(userId)
    val record = LogRecord.fromEvent(event)
    eventValidator.validateAndSend(userId, record, kafkaProducer)
  }

}
