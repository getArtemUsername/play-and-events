package services

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.Configuration

import scala.concurrent.Future

/**
  *
  * ServiceKafkaConsumer class
  * <p/>
  * Description...
  *
  * @author artem klevakin
  */
class ServiceKafkaConsumer(topicNames: Set[String], groupName: String, implicit val mat: Materializer,
                           actorSystem: ActorSystem, configuration: Configuration, handleEvent: String => Unit) {
  val config = configuration.getConfig("kafka")
    .getOrElse(throw new Exception("No config element for kafka!"))
    .underlying

  val consumerSettings = ConsumerSettings(actorSystem, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(config.getString("bootstrap.servers"))
    .withGroupId(groupName)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getString("auto.offset.reset"))
  
  Consumer.committableSource(consumerSettings,
    Subscriptions.topics(topicNames)).mapAsync(1) {
    msg=>
      val event = msg.record.value()
      handleEvent(event)
      Future.successful(msg)
  }.mapAsync(1) { msg =>
    msg.committableOffset.commitScaladsl()
  }.runWith(Sink.ignore)
  
}
