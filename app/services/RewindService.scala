package services

import actors.ValidationActor
import akka.actor.ActorSystem
import dao.{LogDao, Neo4JReadDao}
import play.api.Logger

import scala.util.{Failure, Success}

/**
  * RewindService class
  */
class RewindService(actorSystem: ActorSystem, neo4JReadDao: Neo4JReadDao, logDao: LogDao) {
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
}
