package services

import java.util.UUID
import java.util.concurrent.TimeUnit

import actors.InMemoryReadActor
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import dao.{LogDao, Neo4JReadDao}
import model.{Question, QuestionThread, Tag}
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

  def getQuestionThread(questionId: UUID): Try[Option[QuestionThread]] = {
    val maybeThreadT = neo4JReadDao.getQuestionThread(questionId)
    val namesT = userService.getUserFullNameMap
    for {
      names <- namesT
      maybeThread <- maybeThreadT
    } yield {
      maybeThread.map {
        thread =>
          val sourceQuestion = thread.question
          val sourceAnswers = thread.answers
          val updatedQuestion = sourceQuestion.copy(authorFullName = names
            .get(sourceQuestion.authorId))
          val updatedAnswers = sourceAnswers.map {
            answer =>
              answer.copy(authorFullName = names.get(answer.authorId))
          }
          QuestionThread(updatedQuestion, updatedAnswers)
      }
    }
  }
}
