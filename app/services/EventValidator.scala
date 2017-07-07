package services

import java.util.UUID

import actors.EventStreamActor
import akka.actor.ActorSystem
import com.appliedscala.events.LogRecord

/**
  * EventValidator class
  */
class EventValidator(validationService: ValidationService,
                     actorSystem: ActorSystem) {
  def validatiteAndSend(userId: UUID, event: LogRecord, kafkaProducer: ServiceKafkaProducer): Unit = {
    val maybeErrorMessageF = validationService.validate(event)
    maybeErrorMessageF.map {
      case Some(errorMessage) =>
        val actorSelection = actorSystem.actorSelection(EventStreamActor.userSpecificPathPattern(userId))
        actorSelection ! EventStreamActor.ErrorOccurred(errorMessage)
      case None =>
        kafkaProducer.send(event.encode)
    }
  }
}
