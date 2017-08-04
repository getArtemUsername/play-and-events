package services

import java.util.UUID

import actors.EventStreamActor
import akka.actor.ActorSystem
import com.appliedscala.events.LogRecord

import scala.concurrent.Future

/**
  * EventValidator class
  */
class EventValidator(validationService: ValidationService,
                     actorSystem: ActorSystem) {
  def validateAndSend(userId: UUID, event: LogRecord, kafkaProducer: ServiceKafkaProducer): Future[Option[String]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val maybeErrorMessageF = validationService.validate(event)
    maybeErrorMessageF.map {
      case Some(errorMessage) =>
        val actorSelection = actorSystem
          .actorSelection(EventStreamActor.userSpecificPathPattern(userId))
        actorSelection ! EventStreamActor.ErrorOccurred(errorMessage)
      case None =>
        kafkaProducer.send(event.encode)
    }
    maybeErrorMessageF
  }
}
