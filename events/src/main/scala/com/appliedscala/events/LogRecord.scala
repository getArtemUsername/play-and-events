package com.appliedscala.events

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.JsValue

/**
  *
  * LogRecord class
  * <p/>
  * Description...
  */
case class LogRecord(id: UUID, action: String, data: JsValue, timestamp: DateTime)
