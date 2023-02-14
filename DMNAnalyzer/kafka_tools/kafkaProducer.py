from confluent_kafka import Producer
from kafka_tools import serializers

class KafkaProducer:

    def __init__(self, producer: Producer) -> None:
        self.__producer = producer

    def produce(self, key: str, value : serializers.KafkaObject, topic: str) -> None:
        self.__producer.produce(topic=topic,
                                key=key,
                                value=serializers.serialize(value))

        self.__producer.poll(0)
        self.__producer.flush()