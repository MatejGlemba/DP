from kafka_tools.kafkaConsumer import KafkaConsumer
from kafka_tools.kafkaProducer import KafkaProducer
from kafka_tools import deserializers, serializers
from confluent_kafka import Consumer, Producer


class MessageTopicHandler:
    def __init__(self, uri : str) -> None:
        self.__messageTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic="MESSAGE_DATA")
        self.__messageTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : uri}))
    
    def consume(self):
        return self.__messageTopicConsumer.consume(dataClass=deserializers.MessageData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__messageTopicProducer.produce(key="", value=object, topic="MESSAGE_DATA")
    
    def flush(self):
        self.__messageTopicProducer.flush()

    def commit(self):
        self.__messageTopicConsumer.commit()
    
class BlacklistTopicHandler:
    def __init__(self, uri : str) -> None:
        self.__blacklistTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic="BLACKLIST_DATA")
        self.__blacklistTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : uri}))
    
    def consume(self):
        return self.__blacklistTopicConsumer.consume(dataClass=deserializers.BlacklistData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__blacklistTopicProducer.produce(key="", value=object, topic="BLACKLIST_DATA")
    
    def flush(self):
        self.__blacklistTopicProducer.flush()

    def commit(self):
        self.__blacklistTopicConsumer.commit()

class RoomDataTopicHandler:
    def __init__(self, uri : str) -> None:
        self.__roomDataTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic="ROOM_DATA")
        self.__roomDataTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : uri}))
    
    def consume(self):
        return self.__roomDataTopicConsumer.consume(dataClass=deserializers.RoomData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__roomDataTopicProducer.produce(key="", value=object, topic="ROOM_DATA")

    def flush(self):
        self.__roomDataTopicProducer.flush()

    def commit(self):
        self.__roomDataTopicConsumer.commit()

class RoomDataAndBlacklistTopicHandler:
    def __init__(self, uri : str) -> None:
        self.__roomDataBLTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic=["ROOM_DATA", "BLACKLIST_DATA"])
        self.__roomDataTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : uri}))
    
    def consume(self):
        return self.__roomDataBLTopicConsumer.consumeMore()
    
    def produce(self, object=serializers.KafkaObject):
        if isinstance(object, serializers.BlacklistData):
            #print("blacklist")
            self.__roomDataTopicProducer.produce(key="", value=object, topic="BLACKLIST_DATA")
        else:
            #print("room")
            self.__roomDataTopicProducer.produce(key="", value=object, topic="ROOM_DATA")
    def flush(self):
        self.__roomDataTopicProducer.flush()

    def commit(self):
        self.__roomDataBLTopicConsumer.commit()

class GlobalHandler:
    def __init__(self, uri : str) -> None:
        self.__globalConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic=["ROOM_DATA", "BLACKLIST_DATA", "MESSAGE_DATA"])
    
    def consume(self):
        return self.__globalConsumer.consumeMore()

    def commit(self):
        self.__globalConsumer.commit()

class MessageOutputsTopicHandler:
    def __init__(self, uri : str) -> None:
        self.__messageOutputsTopicConsumer: KafkaConsumer = KafkaConsumer(consumer=Consumer({'bootstrap.servers': uri, 'group.id': 'foo', 'auto.offset.reset': 'earliest'}), topic="MESSAGE_OUTPUT")
        self.__messageOutputsTopicProducer: KafkaProducer = KafkaProducer(producer=Producer({'bootstrap.servers' : uri}))
    
    def consume(self):
        return self.__messageOutputsTopicConsumer.consume(dataClass=deserializers.MessageOutputs)
    
    def produce(self, object=serializers.KafkaObject):
        self.__messageOutputsTopicProducer.produce(key="", value=object, topic="MESSAGE_OUTPUT")

    def flush(self):
        self.__messageOutputsTopicProducer.flush()

    def commit(self):
        self.__messageOutputsTopicConsumer.commit()
    
