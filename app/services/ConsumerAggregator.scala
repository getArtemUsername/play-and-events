package services

/**
  * ConsumerAggregator class
  */
class ConsumerAggregator(tagEventConsumer: TagEventConsumer, 
                         logRecordConsumer: LogRecordConsumer,
                         userEventConsumer: UserEventConsumer,
                         questionEventConsumer: QuestionEventConsumer)
