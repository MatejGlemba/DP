from typing import List
from confluent_kafka import Consumer, Message
from kafka_tools import deserializers



class KafkaConsumer:
    def __init__(self, consumer: Consumer, topic) -> None:
        self.consumer = consumer
        self.setTopics(topic)

    def setTopics(self, topic):
        if isinstance(topic, str):
            #print("subcribe topic")
            self.consumer.subscribe([topic])
        else:
            #print("subcribe more topics")
            self.consumer.subscribe(topic)

    def consume(self, dataClass=deserializers.KafkaDeserializerObject) -> Message:
        msg = self.consumer.poll(1.0)
        if msg:
           # print(msg)
            if msg.error():
                print("Error while consuming message :", msg.error())
            else:
                msg = msg.value()
                #print(msg)
                return deserializers.deserialize(jsonValue=msg, dataClass=dataClass)
        
    def consumeMore(self) -> Message:
        msg = self.consumer.poll(1.0)
        if msg:
          #  print(msg)
            if msg.error():
                print("Error while consuming message :", msg.error())
            else:
                msg = msg.value()
                msgDict = eval(msg)
               # print(msgDict)
                if 'notes' in msgDict.keys():
                    # Blacklist data
                    return deserializers.deserialize(jsonValue=msg, dataClass=deserializers.UserData)
                elif 'description' in msgDict.keys():
                    # Room data
                    return deserializers.deserialize(jsonValue=msg, dataClass=deserializers.RoomData)
                else:
                    # Message Data
                    return deserializers.deserialize(jsonValue=msg, dataClass=deserializers.MessageData)