package services

import akka.actor.ActorSystem
import akka.stream.Materializer
import ch.qos.logback.classic.spi.Configurator
import com.appliedscala.events.LogRecord
import dao.LogDao
import play.api.Configuration

/**
  * LogRecordConsumer class
  */
class LogRecordConsumer(logDao: LogDao, actorSystem: ActorSystem, configuration: Configuration,
                        materializer: Materializer) {
  val topics: Set[String] = Seq("tags", "users").toSet
  val serviceKafkaConsumer = new ServiceKafkaConsumer(topics, "logs", materializer, actorSystem,
    configuration, handleEvent)
  
  private def handleEvent(event: String): Unit = {
    val maybeGenericEnvelope = LogRecord.decode(event)
    maybeGenericEnvelope.foreach {
      env =>
        logDao.insertLogRecord(env)
    }
  }

}
