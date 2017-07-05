package com.appliedscala.events

import java.util.UUID

import play.api.libs.json.Json

/**
  * UserActivated class
  */
case class UserActivated(id: UUID) extends EventData {
  override def action = UserActivated.actionName
  override def json = Json.writes[UserActivated].writes(this)
}

object UserActivated {
  val actionName = "user-activated"
  implicit val reads = Json.reads[UserActivated]
}
