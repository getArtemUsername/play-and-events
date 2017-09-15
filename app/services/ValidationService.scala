package services

import java.util.concurrent.TimeUnit

import actors.ValidationActor
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import com.appliedscala.events.LogRecord

import scala.concurrent.Future

/**
  * ValidationService class
  */
class ValidationService(actorSystem: ActorSystem) {

  val validationActor = actorSystem.actorOf(ValidationActor.props(), ValidationActor.name)

  def validate(event: LogRecord): Future[Option[String]] = {
    implicit val timeout = Timeout.apply(5, TimeUnit.SECONDS)
    (validationActor ? ValidationActor.ValidationEventRequest(event)).mapTo[Option[String]]
  }

  def refreshState(events: Seq[LogRecord], fromScratch: Boolean): Future[Option[String]] = {
    implicit val timeout = Timeout(5, TimeUnit.SECONDS)
    (validationActor ? ValidationActor.RefreshStateCommand(events, fromScratch)).mapTo[Option[String]]
  }
}
