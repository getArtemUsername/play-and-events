package com.appliedscala.events

import java.util.UUID

import play.api.libs.json.Json

/**
  * UserActivated class
  */
case class UserDeactivated(id: UUID) extends EventData {
  override def action = UserDeactivated.actionName
  override def json = Json.writes[UserDeactivated].writes(this)
}

object UserDeactivated {
  val actionName = "user-deactivated"
  implicit val reads = Json.reads[UserDeactivated]
}


