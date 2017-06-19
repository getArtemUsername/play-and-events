package services

import java.util.concurrent.TimeUnit

import actors.InMemoryReadActor
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import dao.LogDao
import model.Tag
import play.api.Logger

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  *
  * ReadService class
  * <p/>
  * Description...
  *
  */
class ReadService(actorSystem: ActorSystem, logDao: LogDao) {
  init()

  private def init(): Unit = {
    val logRecordsT = logDao.getLogRecords

    logRecordsT match {
      case Failure(th) =>
        Logger.error("Error while init the read service", th)
        throw th
      case Success(logRecords) =>
        val actor = actorSystem
          .actorOf(InMemoryReadActor.props(logRecords), InMemoryReadActor.name)
        actor ! InMemoryReadActor.InitializeState
    }
  }

  def getTags: Future[Seq[Tag]] = {
    implicit val timeout = Timeout(5, TimeUnit.MINUTES)
    val actor = actorSystem.actorSelection(InMemoryReadActor.path)
    (actor ? InMemoryReadActor.GetTags).mapTo[Seq[Tag]]
  }
}
