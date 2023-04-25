from kafka_tools.kafkaConsumer import KafkaConsumer
from kafka_tools.kafkaProducer import KafkaProducer
from kafka_tools import deserializers, serializers
from confluent_kafka import Consumer, Producer


class MessageTopicHandler:
    def __init__(self, uri : str) -> None:
        self.messageTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic="MESSAGE_DATA")
        self.__messageTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : uri}))
    
    def consume(self):
        return self.messageTopicConsumer.consume(dataClass=deserializers.MessageData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__messageTopicProducer.produce(key="", value=object, topic="MESSAGE_DATA")
    
    def flush(self):
        self.__messageTopicProducer.flush()
    
class RoomAndUserDataTopicHandler:
    def __init__(self, uri : str) -> None:
        self.roomAndUserDataTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic=["ROOM_DATA", "USER_DATA"])
        self.__roomAndUserDataTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : uri}))
    
    def consume(self):
        return self.roomAndUserDataTopicConsumer.consumeMore()
    
    def produce(self, object=serializers.KafkaObject):
        if isinstance(object, serializers.UserData):
            #print("user data")
            self.__roomAndUserDataTopicProducer.produce(key="", value=object, topic="USER_DATA")
        else:
            #print("room data")
            self.__roomAndUserDataTopicProducer.produce(key="", value=object, topic="ROOM_DATA")

    def flush(self):
        self.__roomAndUserDataTopicProducer.flush()


class TopicHandler:
    def __init__(self, uri : str) -> None:
        #self.topicHandler: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic=["ROOM_DATA", "USER_DATA", "MESSAGE_DATA"])
        self.topicHandler: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic=["MESSAGE"])
        
    def consume(self):
        return self.topicHandler.consumeMore()
    
