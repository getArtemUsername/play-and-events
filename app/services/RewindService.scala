package services

import actors.{EventStreamActor, ValidationActor}
import akka.actor.ActorSystem
import dao.{LogDao, Neo4JReadDao}
import model.ServerSentMessage
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.JsBoolean

import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

/**
  * RewindService class
  */
class RewindService(actorSystem: ActorSystem, validationService: ValidationService,
                    neo4JReadDao: Neo4JReadDao, logDao: LogDao) {
  def refreshState(): Unit = {
    val eventsT = logDao.getLogRecords
    eventsT match {
      case Success(events) =>
        val validationActor = actorSystem.actorSelection(ValidationActor.path)
        validationActor ! ValidationActor.RefreshStateCommand(events)
        neo4JReadDao.refreshState(events, fromScratch = true)
      case Failure(th) =>
        Logger.error("Error occurred while rewinding the state", th)
    }
  }

  def refreshState(upTo: Option[DateTime]): Try[Unit] = {
    val resultT = recreateState(upTo)
    resultT match {
      case Failure(th) ⇒
        Logger.error("Error occurred while rewinding the state", th)
      case Success(_) ⇒
        Logger.info("The state was successfully rebuild")
        val update = ServerSentMessage.create("stateRebuild",
          JsBoolean(true))
        val esActor = actorSystem.actorSelection(EventStreamActor.pathPattern)
        esActor ! EventStreamActor.DataUpdated(update.json)
    }
    resultT
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  private def recreateState(upTo: Option[DateTime]): Try[Unit] = Try {
    val timeout = 5.seconds
    val bufferSize = 200

    val readResetT = neo4JReadDao.refreshState(Nil, fromScratch = true)
    val validationResetF = validationService.refreshState(Nil, fromScratch = true)
    val validationResetT = message2Try(Await.result(validationResetF, timeout))

    val resetT = for {
      readReset ← readResetT; validationReset ← validationResetT
    } yield (readReset, validationReset)

    resetT match {
      case Success(_) ⇒ ()
      case Failure(th) ⇒ throw th
    }

    val rewindT = logDao.iterateLogRecords(upTo)(bufferSize) { events ⇒
      val readRefreshT = neo4JReadDao.refreshState(events, fromScratch = false)
      val validationRefreshF = validationService.refreshState(events, fromScratch = false)
      val validationRefreshT = message2Try(Await.result(validationRefreshF, timeout))

      val refreshT = for {
        readRefresh ← readRefreshT; validationRefresh ← validationRefreshT
      } yield (readRefresh, validationRefresh)

      refreshT match {
        case Success(_) ⇒ ()
        case Failure(th) ⇒ throw th
      }
    }

    rewindT match {
      case Success(_) ⇒ ()
      case Failure(th) ⇒ throw th
    }

  }

  private def message2Try(errorMessage: Option[String]): Try[Unit] = {
    errorMessage match {
      case None ⇒ Success(())
      case Some(msg) ⇒ Failure(new Exception(msg))
    }
  }

}
