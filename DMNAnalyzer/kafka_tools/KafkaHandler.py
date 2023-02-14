from kafka_tools import kafkaConsumer, kafkaProducer, deserializers, serializers
from confluent_kafka import Consumer, Producer


class MessageTopicHandler:
    def __init__(self) -> None:
        self.__messageTopicConsumer: kafkaConsumer = kafkaConsumer(consumer=Consumer({'bootstrap.servers': 'localhost:9094', 'group.id': 'dh', 'auto.offset.reset': 'earliest'}), topic="message_data")
        self.__messageTopicProducer: kafkaProducer = kafkaProducer(producer=Producer({'bootstrap.servers' : 'localhost:9094'}))
    
    def consume(self):
        return self.__messageTopicConsumer.consume(dataClass=deserializers.MessageData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__messageTopicProducer.produce(key="", value=object, topic="message_data")
    

class BlacklistTopicHandler:
    def __init__(self) -> None:
        self.__messageTopicConsumer: kafkaConsumer = kafkaConsumer(consumer=Consumer({'bootstrap.servers': 'localhost:9094', 'group.id': 'dh', 'auto.offset.reset': 'earliest'}), topic="blacklist_data")
        self.__messageTopicProducer: kafkaProducer = kafkaProducer(producer=Producer({'bootstrap.servers' : 'localhost:9094'}))
    
    def consume(self):
        return self.__messageTopicConsumer.consume(dataClass=deserializers.BlacklistData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__messageTopicProducer.produce(key="", value=object, topic="blacklist_data")

class ImageTopicHandler:
    def __init__(self) -> None:
        self.__messageTopicConsumer: kafkaConsumer = kafkaConsumer(consumer=Consumer({'bootstrap.servers': 'localhost:9094', 'group.id': 'dh', 'auto.offset.reset': 'earliest'}), topic="image_data")
        self.__messageTopicProducer: kafkaProducer = kafkaProducer(producer=Producer({'bootstrap.servers' : 'localhost:9094'}))
    
    def consume(self):
        return self.__messageTopicConsumer.consume(dataClass=deserializers.ImageData)
    
    def produce(self, object=serializers.KafkaObject):
        self.__messageTopicProducer.produce(key="", value=object, topic="image_data")

class MessageOutputsTopicHandler:
    def __init__(self) -> None:
        self.__messageTopicConsumer: kafkaConsumer = kafkaConsumer(consumer=Consumer({'bootstrap.servers': 'localhost:9094', 'group.id': 'dh', 'auto.offset.reset': 'earliest'}), topic="message_outputs")
        self.__messageTopicProducer: kafkaProducer = kafkaProducer(producer=Producer({'bootstrap.servers' : 'localhost:9094'}))
    
    def consume(self):
        return self.__messageTopicConsumer.consume(dataClass=deserializers.MessageOutputs)
    
    def produce(self, object=serializers.KafkaObject):
        self.__messageTopicProducer.produce(key="", value=object, topic="message_outputs")
