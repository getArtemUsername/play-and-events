package dao

import java.util.UUID

import com.appliedscala.events.{LogRecord, TagCreated, TagDeleted}
import model.Tag

/**
  *
  * InMemoryReadDao class
  * <p/>
  * Description...
  *
  */
class InMemoryReadDao(records: Seq[LogRecord]) {
  import scala.collection.mutable.{Map => MMAp}
  val tags = MMAp.empty[UUID, Tag]
  
  def init(): Unit = records.foreach(processEvent)
  
  def getTags: Seq[Tag] = {
    tags.values.toList.sortWith(_.text < _.text)
  }
  
  def processEvent(record: LogRecord): Unit = {
    record.action match {
      case TagCreated.actionName =>
        val event = record.data.as[TagCreated]
        tags += (event.id -> Tag(event.id, event.text))
      case TagDeleted.actionName =>
        val event = record.data.as[TagDeleted]
        tags -= event.id
      case _ => ()
    }
  }
}
