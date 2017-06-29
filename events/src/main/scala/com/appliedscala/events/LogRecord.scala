package com.appliedscala.events

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Reads, Writes}

/**
  *
  * LogRecord class
  * <p/>
  * Description...
  */
case class LogRecord(id: UUID, action: String, data: JsValue, timestamp: DateTime) {
  def encode: String = Json.toJson(this)(LogRecord.writes).toString
}

object LogRecord {
  val writes: Writes[LogRecord] = Json.writes[LogRecord]
  val reads: Reads[LogRecord] = Json.reads[LogRecord]

  def decode(str: String): Option[LogRecord] = {
    Json.parse(str).asOpt[LogRecord](reads)
  }
}


