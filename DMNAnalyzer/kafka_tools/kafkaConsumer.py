from typing import List
from confluent_kafka import Consumer, Message
from kafka_tools import deserializers



class KafkaConsumer:
    def __init__(self, consumer: Consumer, topic, poll_timeout: float = 1.0) -> None:
        self.__consumer = consumer
        self.__poll_timeout: float = poll_timeout
        self.__setTopics(topic)

    def __setTopics(self, topic):
        if isinstance(topic, str):
            #print("subcribe topic")
            self.__consumer.subscribe([topic])
        else:
            #print("subcribe more topics")
            self.__consumer.subscribe(topic)

    def consume(self, dataClass=deserializers.KafkaDeserializerObject) -> Message:
        msg = self.__consumer.poll(timeout=self.__poll_timeout)
        if msg:
            msg = msg.value()
            print(msg)
            return deserializers.deserialize(jsonValue=msg, dataClass=dataClass)
        
    def consumeMore(self, dataClasses: List) -> Message:
        msg = self.__consumer.poll(timeout=self.__poll_timeout)
        if msg:
            msg = msg.value()
            msgDict = eval(msg)
            if 'userID' in msgDict.keys():
                return deserializers.deserialize(jsonValue=msg, dataClass=dataClasses[0])
            else:
                return deserializers.deserialize(jsonValue=msg, dataClass=dataClasses[1])
    
    def commit(self):
        self.__consumer.commit(asynchronous=True)