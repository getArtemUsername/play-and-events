package dao

import java.util.UUID

import com.appliedscala.events._
import model.{Question, Tag}
import org.joda.time.DateTime
import services.{Neo4JQuery, Neo4JQueryExecutor, Neo4JUpdate}
import utils.BaseTypes

import scala.util.{Failure, Success, Try}

/**
  * Neo4JReadDao class
  */
class Neo4JReadDao(queryExecutor: Neo4JQueryExecutor) {


  def handleEvent(event: LogRecord): Unit = {
    val updates = prepareUpdates(event)
    updates.queries.foreach {
      update =>
        queryExecutor.executeUpdate(update)
    }
  }
  
  def handleEventWithUpdate(logRecord: LogRecord)(updateBlock: Option[UUID] => Unit): Unit = {
    val updateInfo = prepareUpdates(logRecord)
    updateInfo.queries.foreach {
      update => 
        queryExecutor.executeUpdate(update)
    }
    updateBlock(updateInfo.updateId)
  }

  private def prepareUpdates(record: LogRecord): Neo4JUpdate = {
    record.action match {
      case UserActivated.actionName =>
        val decoded = record.data.as[UserActivated]
        Neo4JUpdate(activateUser(decoded.id))
      case UserDeactivated.actionName =>
        val decoded = record.data.as[UserDeactivated]
        Neo4JUpdate(deactivateUser(decoded.id))
      case TagCreated.actionName =>
        val decoded = record.data.as[TagCreated]
        Neo4JUpdate(createTag(decoded.id, decoded.text))
      case TagDeleted.actionName =>
        val decoded = record.data.as[TagDeleted]
        Neo4JUpdate(deleteTag(decoded.id))
      case QuestionCreated.actionName =>
        val decoded = record.data.as[QuestionCreated]
        Neo4JUpdate(createQuestion(decoded.questionId, decoded.title, decoded.details, decoded.createdBy,
          decoded.tags, decoded.created))
      case QuestionDeleted.actionName =>
        val decoded = record.data.as[QuestionDeleted]
        Neo4JUpdate(deleteQuestion(decoded.questionId))
      case _ => Neo4JUpdate(Nil)
    }
  }

  private def createTag(tagId: UUID, tagText: String): Seq[Neo4JQuery] = {
    val update = """create (:Tag { tagId: {tagId}, tagText: {tagText} })"""
    Seq(Neo4JQuery(update, Map("tagId" -> tagId.toString, "tagText" -> tagText)))
  }

  private def deleteTag(tagId: UUID): Seq[Neo4JQuery] = {
    val update = """match (t:Tag { tagId: {tagId} }) detach delete t"""
    Seq(Neo4JQuery(update, Map("tagId" -> tagId.toString)))
  }

  private def deleteQuestion(questionId: UUID): Seq[Neo4JQuery] = {
    val update = """match (q: Question { id: {questionId} }) detach delete q"""
    Seq(Neo4JQuery(update, Map("questionId" -> questionId.toString)))
  }

  private def activateUser(userId: UUID): Seq[Neo4JQuery] = {
    val update = """create (:User { id: {userId} })"""
    Seq(Neo4JQuery(update, Map("userId" -> userId.toString)))
  }

  private def deactivateUser(userId: UUID): Seq[Neo4JQuery] = {
    val update = """match (u:User { id: {userId} }) detach delete u"""
    Seq(Neo4JQuery(update, Map("userId" -> userId.toString)))
  }

  private def createQuestionUserQuery(questionId: UUID, title: String, details: Option[String], addedBy: UUID,
                                      created: DateTime): Neo4JQuery = {
    val createdFmt = BaseTypes.formatISO8601(created)
    val detailsPart = details.getOrElse("")
    val createQuestion =
      """
        |match (u: User { id: {userId} } ) 
        |create (q: Question {title: {title}, id: {questionId}, created: {created}, details: {details} } ), 
        |(q)-[wb: WRITTEN]->(u)-[w:WROTE]->(q)""".stripMargin
    Neo4JQuery(createQuestion, Map("userId" -> addedBy.toString, "title" -> title,
      "questionId" -> questionId.toString, "created" -> createdFmt, "details" -> detailsPart))
  }

  private def createQuestionTagQuery(questionId: UUID, tagIds: Seq[UUID]): Neo4JQuery = {
    val tags = tagIds.map { id => id.toString }.toArray
    val updateTagRelationships =
      """match (t:Tag),(q:Question { id: {questionId} }) 
        |where t.tagId in {tags} 
        |create (q)-[b:BELONGS]->(t)-[c:CONTAINS]->(q)""".stripMargin
    Neo4JQuery(updateTagRelationships, Map("questionId" -> questionId.toString, "tags" -> tags))
  }

  private def createQuestion(questionId: UUID, title: String, details: Option[String], addedBy: UUID, tagIds: Seq[UUID],
                             created: DateTime): Seq[Neo4JQuery] = {
    val userQuery = createQuestionUserQuery(questionId, title, details, addedBy, created)
    val tagQuery = createQuestionTagQuery(questionId, tagIds)
    Seq(userQuery, tagQuery)
  }

  def getQuestion(questionId: UUID): Try[Option[Question]] = {
    val queryQuestion =
      """
         MATCH (u:User)-[:WROTE]->(q:Question {id: {questionId} })-[:BELONGS]->(t: Tag)
         RETURN q.title as title, q.details as details, q.id as question_id,
         q.created as created, u.id as user_id, collect(distinct t) as tags
      """
    val questionRecordsT = queryExecutor.executeQuery(Neo4JQuery(queryQuestion,
      Map("questionId" -> questionId.toString)))
    val questionT = questionRecordsT.map { records =>
      records.headOption.map { record =>
        val detailsStr = record.get("details").asString()
        val details = if (detailsStr.isEmpty) None else Some(detailsStr)
        val title = record.get("title").asString()
        val createdStr = record.get("created").asString()
        val questionId = UUID.fromString(record.get("question_id").asString())
        val authorId = UUID.fromString(record.get("user_id").asString())
        val created = BaseTypes.parseISO8601(createdStr)
        val tagsSize = record.get("tags").size()
        val tagsIndexes = 0.until(tagsSize).toList
        val tags = tagsIndexes.map { index =>
          val values = record.get("tags").get(index).asMap()
          val code = UUID.fromString(values.get("tagId").asInstanceOf[String])
          val text = values.get("tagText").asInstanceOf[String]
          Tag(code, text)
        }
        val question = Question(questionId, title, details, tags, created, authorId, None)
        question
      }
    }
    questionT
  }

  def getQuestions: Try[Seq[Question]] = {
    val query =
      """match (q:Question) return q"""
    val recordsT = queryExecutor.executeQuery(Neo4JQuery.simple(query))
    recordsT.map {
      records =>
        records
          .map { r => r.get("q") }.map {
          record =>
            val id = record.get("id").asString
            val title = record.get("title").asString
            val details = Option(record.get("details").asString)
            val tags = getTagsForQuestion(id)
            val created = record.get("created").asString
            val authorId = getAuthorId(id)
            val authorFullName = record.get("authorFullName").asString
            Question(UUID.fromString(id), title, details, tags, BaseTypes.parseISO8601(created),
              UUID.fromString(authorId), Option(authorFullName))
        }
    }
  }

  def getAllTags: Try[Seq[Tag]] = {
    val query =
      """match (t:Tag) return t.tagId as tagId, 
        |t.tagText as tagText order by tagText""".stripMargin
    val recordsT = queryExecutor.executeQuery(Neo4JQuery.simple(query))
    recordsT.map {
      records =>
        records.map {
          record =>
            val id = record.get("tagId").asString
            val text = record.get("tagText").asString
            Tag(UUID.fromString(id), text)
        }
    }
  }

  private def getTagsForQuestion(questionId: String): Seq[Tag] = {
    val query =
      """match (t:Tag), (q:Question) where (t)-[:BELONGS]->(q) and q.id ={questionId} return t"""
    val recordsT = queryExecutor.executeQuery(Neo4JQuery(query,
      Map("questionId" -> questionId.toString)))
    recordsT.map {
      records =>
        records.map { t => t.get("t") }.map {
          record =>
            val id = record.get("id").asString
            val text = record.get("text").asString
            Tag(UUID.fromString(id), text)
        }
    } match {
      case Success(seq) => seq
      case Failure(_) => Seq()
    }
  }

  private def getAuthorId(questionId: String): String = {
    val query =
      """
        |match (u:User)-[:WROTE]->(q:Question) where q.id = {questionId} return u.id as authorId
      """.stripMargin
    val recordsT = queryExecutor.executeQuery(Neo4JQuery(query,
      Map("questionId" -> questionId.toString)))
    recordsT.map {
      records =>
        records.map {
          record =>
            record.get("authorId").asString()
        }
    } match {
      case Success(head :: _) => head
      case _ => "no author id"
    }
  }

  private def rebuildState(events: Seq[LogRecord]): Try[Unit] = {
    val updates = events.flatMap {
      event =>
        prepareUpdates(event).queries
    }
    queryExecutor.executeBatch(updates)
  }

  def refreshState(events: Seq[LogRecord], fromScratch: Boolean): Try[Unit] = {
    for {
      _ <- clear(fromScratch)
      _ <- rebuildState(events)
    } yield ()
  }

  private def clear(fromScratch: Boolean): Try[Unit] = {
    if (fromScratch) {
      val update = "MATCH (all) detach delete all"
      queryExecutor.executeUpdate(Neo4JQuery.simple(update)).map(_ => ())
    } else Try.apply(())
  }
}
