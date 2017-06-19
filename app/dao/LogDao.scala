package dao

import java.util.UUID

import com.appliedscala.events.LogRecord
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
        sql"""INSERT INTO events(record_id, aciton_name, event_data, timestamp) 
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

  private def rs2LogRecord(rs: WrappedResultSet): LogRecord = {
    LogRecord(UUID.fromString(rs.string("record_id")), rs.string("action_name"),
      Json.parse(rs.string("event_data")), rs.jodaDateTime("timestamp"))
  }


}
