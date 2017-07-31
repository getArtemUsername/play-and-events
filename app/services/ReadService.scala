package services

import java.util.concurrent.TimeUnit

import actors.InMemoryReadActor
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import dao.{LogDao, Neo4JReadDao}
import model.{Question, Tag}
import play.api.Logger

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * ReadService class
  */
class ReadService(neo4JReadDao: Neo4JReadDao, actorSystem: ActorSystem, logDao: LogDao,
                  userService: UserService) {

  def getAllTags: Try[Seq[Tag]] = {
    neo4JReadDao.getAllTags
  }
  
  def getAllQuestions: Try[Seq[Question]] = {
    val namesT = userService.getUserFullNameMap
    val questionsT = neo4JReadDao.getQuestions
    for {
      names <- namesT
      questions <- questionsT
    } yield {
      questions.map {
        question =>
          question.copy(authorFullName = names.get(question.authorId))
      }
    }
  }
}
