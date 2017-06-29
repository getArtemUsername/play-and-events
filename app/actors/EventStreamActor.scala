package actors

import java.util.UUID

import akka.actor.Actor.Receive
import akka.actor.Props
import akka.stream.actor.ActorPublisher
import play.api.libs.json.{JsObject, JsString, JsValue}

/**
  *
  * EventStreamActor class
  * <p/>
  * Description...
  *
  * @author artem klevakin
  */

class EventStreamActor extends ActorPublisher[JsValue] {
  import EventStreamActor._
  import akka.stream.actor.ActorPublisherMessage._
  
  override def receive: Receive = {
    case DataUpdated(js) => onNext(js)
    case ErrorOccurred(msg) => onNext(JsObject(Seq("error" -> JsString(msg))))
    case Request(_) => ()
    case Cancel => context.stop(self)
  }
}

object EventStreamActor {
  def props = Props(new EventStreamActor)
  
  case class DataUpdated(jsValue: JsValue)
  case class ErrorOccurred(message: String)
  
  val name = "event-stream-actor"
  val pathPattern = s"/user/$name-*"
  
  def name(maybeUserId: Option[UUID]): String = {
    val randomPart = UUID.randomUUID().toString.split("-").apply(0)
    val userPart = maybeUserId.map(_.toString).getOrElse("unregistered")
    s"$name-$userPart-$randomPart"
  }
  
  def userSpecificPathPattern(userId: UUID): String = {
    s"/user/$name-${userId.toString}-*"
  }
}
