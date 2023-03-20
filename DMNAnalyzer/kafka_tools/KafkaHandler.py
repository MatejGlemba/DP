from kafka_tools.kafkaConsumer import KafkaConsumer
from kafka_tools.kafkaProducer import KafkaProducer
from kafka_tools import deserializers, serializers
from confluent_kafka import Consumer, Producer


class MessageTopicHandler:
    def __init__(self) -> None:
        self.__messageTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': 'localhost:9094', 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic="MESSAGE_DATA")
        self.__messageTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : 'localhost:9094'}))
    
    def consume(self):
        return self.__messageTopicConsumer.consume(dataClass=deserializers.MessageData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__messageTopicProducer.produce(key="", value=object, topic="MESSAGE_DATA")
    

class BlacklistTopicHandler:
    def __init__(self) -> None:
        self.__blacklistTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': 'localhost:9094', 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic="BLACKLIST_DATA")
        self.__blacklistTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : 'localhost:9094'}))
    
    def consume(self):
        return self.__blacklistTopicConsumer.consume(dataClass=deserializers.BlacklistData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__blacklistTopicProducer.produce(key="", value=object, topic="BLACKLIST_DATA")

class RoomDataTopicHandler:
    def __init__(self) -> None:
        self.__roomDataTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': 'localhost:9094', 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic="ROOM_DATA")
        self.__roomDataTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : 'localhost:9094'}))
    
    def consume(self):
        return self.__roomDataTopicConsumer.consume(dataClass=deserializers.RoomData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__roomDataTopicProducer.produce(key="", value=object, topic="ROOM_DATA")

class MessageOutputsTopicHandler:
    def __init__(self) -> None:
        self.__messageOutputsTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': 'localhost:9094', 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic="MESSAGE_OUTPUT")
        self.__messageOutputsTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : 'localhost:9094'}))
    
    def consume(self):
        return self.__messageOutputsTopicConsumer.consume(dataClass=deserializers.MessageOutputs)
    
    def produce(self, object=serializers.KafkaObject):
        self.__messageOutputsTopicProducer.produce(key="", value=object, topic="MESSAGE_OUTPUT")
