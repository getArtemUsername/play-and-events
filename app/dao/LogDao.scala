package dao

import java.util.UUID

import com.appliedscala.events.LogRecord
import org.joda.time.DateTime
import play.api.libs.json.Json
import scalikejdbc._

import scala.util.Try

/**
  *
  * LogDao class
  * <p/>
  * Description...
  *
  */
class LogDao {
  def insertLogRecord(event: LogRecord): Try[Unit] = Try {
    NamedDB('eventstore).localTx {
      implicit session =>
        val jsonStr = event.data.toString
        sql"""INSERT INTO logs(record_id, action_name, event_data, timestamp) 
              VALUES (${event.id}, ${event.action}, ${jsonStr}, ${event.timestamp})
           """.update.apply
    }
  }

  def getLogRecords: Try[Seq[LogRecord]] = Try {
    NamedDB('eventstore).readOnly {
      implicit session =>
        sql"""SELECT * FROM logs ORDER BY TIMESTAMP""".map(rs2LogRecord).list.apply
    }
  }

  import scala.collection.mutable.ListBuffer

  def iterateLogRecords(maybeUpTo: Option[DateTime])(chunkSize: Int)
                       (handler: (Seq[LogRecord]) => Unit): Try[Unit] = Try {
    NamedDB('eventstore).readOnly { implicit session =>
      val upTo = maybeUpTo.getOrElse(DateTime.now())
      val buffer = ListBuffer[LogRecord]()
      sql"SELECT * FROM logs WHERE timestamp <= $upTo ORDER BY TIMESTAMP"
        .foreach { wrs =>
          val event = rs2LogRecord(wrs)
          buffer.append(event)
          if (buffer.size >= chunkSize) {
            handler(buffer.toList)
            buffer.clear()
          }
        }
      if (buffer.nonEmpty) {
        handler(buffer.toList)
      }
    }

  }

  private def rs2LogRecord(rs: WrappedResultSet): LogRecord = {
    LogRecord(UUID.fromString(rs.string("record_id")), rs.string("action_name"),
      Json.parse(rs.string("event_data")), rs.jodaDateTime("timestamp"))
  }
}
