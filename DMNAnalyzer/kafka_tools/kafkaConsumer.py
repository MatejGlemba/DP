from confluent_kafka import Consumer, Message
from kafka_tools import deserializers


class KafkaConsumer:

    def __init__(self, consumer: Consumer, topic: str, poll_timeout: float = 5.0) -> None:
        self.__topic: str = topic
        self.__consumer = consumer
        self.__poll_timeout: float = poll_timeout
        self.__consumer.subscribe([self.__topic])

    def consume(self, dataClass=deserializers.KafkaDeserializerObject) -> Message:
        msg = self.__consumer.poll(self.__poll_timeout)
        print(msg)
        if msg:
            msg = msg.value()
            return deserializers.deserialize(jsonValue=msg, dataClass=dataClass)
