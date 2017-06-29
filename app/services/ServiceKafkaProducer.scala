package services

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import play.api.Configuration

/**
  *
  * ServiceKafkaProducer class
  * <p/>
  * Description...
  *
  * @author artem klevakin
  */
class ServiceKafkaProducer(topicName: String, actorSystem: ActorSystem,
                           configuration: Configuration) {
  val bootstrapServers: String = configuration.getString("kafka.bootstrap.servers")
    .getOrElse(throw new Exception("No config for kafka bootstrap servers!"))

  val producerSettings: ProducerSettings[String, String] = ProducerSettings(actorSystem, new StringSerializer,
    new StringSerializer)
    .withBootstrapServers(bootstrapServers)

  val producer: KafkaProducer[String, String] = producerSettings.createKafkaProducer

  def send(logRecordStr: String): Unit = {
    producer.send(new ProducerRecord(topicName, logRecordStr))
  }

}
