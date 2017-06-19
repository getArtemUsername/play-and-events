package com.appliedscala.events

import play.api.libs.json.JsValue

/**
  *
  * EventData class
  * <p/>
  * Description...
  *
  */
trait EventData {
  def action: String
  def json: JsValue
}
