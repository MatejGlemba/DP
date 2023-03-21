from confluent_kafka import Producer
from kafka_tools import serializers

class KafkaProducer:

    def __init__(self, producer: Producer) -> None:
        self.__producer = producer

    def acked(err, msg, ss):
        if err is not None:
            print("Failed to deliver message: %s: %s" % (str(msg), str(err)))
        else:
            print("Message produced: %s" % (str(msg)))

    def produce(self, key: str, value : serializers.KafkaObject, topic: str) -> None:
        self.__producer.poll(0)
        self.__producer.produce(topic=topic,
                                key=key,
                                value=serializers.serialize(value),
                                callback=self.acked)

        #self.__producer.poll(1)
        self.__producer.flush()