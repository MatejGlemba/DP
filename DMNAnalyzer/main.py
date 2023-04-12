import json
from multiprocessing import Process
from typing import Dict, List, Tuple
from kafka_tools import KafkaHandler
from kafka_tools.deserializers import KafkaDeserializerObject, MessageData, BlacklistData, MessageOutputs, RoomData
from utils.crypto import Crypto
from utils.messagesCounter import Counter
from ai_tools import hatespeechChecker, spamChecker, text_Preprocessing, topic_modeling
from DB_tools.MongoHandler import DBHandler, MessagesDBHandler, RoomDBHandler, BlacklistDBHandler#, EntityRoomDBHandler, EntityUserDBHandler
from DB_tools.InfluxDBHandler import EntityRoomDBHandler, EntityUserDBHandler, InfluxDBHandler
from DB_tools.EntityModels import RoomEntity, UserEntity

def messageAnalyzer():
    messageTopicHandler = KafkaHandler.MessageTopicHandler()
    messageOutputHandler = KafkaHandler.MessageOutputsTopicHandler()
    dbHandler = DBHandler()
    influxDBHandler = InfluxDBHandler()
    messagesDBHandler : MessagesDBHandler = dbHandler.getMessagesDBHandler()
    entityRoomDBHandler: EntityRoomDBHandler = influxDBHandler.getEntityRoomDBHandler() 
    entityUserDBHandler: EntityUserDBHandler = dbHandler.getEntityUserDBHandler()
    counter : Counter = Counter()

    while True:
        msgData : MessageData = messageTopicHandler.consume()
        print("Message analyzer - waiting for data")
        #messageTopicHandler.commit()

        if msgData:
            print("input: ", msgData.__dict__)
            counter.inc_counter(msgData.roomID, msgData.qrcodeID)

            # save incoming data into MongoDB
            if messagesDBHandler.readMessagesDataForRoom([msgData.roomID, msgData.qrcodeID]):
                messagesDBHandler.updateMessagesData([msgData.roomID, msgData.qrcodeID], msgData.data)
            else:
                messagesDBHandler.insertMessageData(msgData)
            
            # check hate speech, notify app server, save user data 
            if hatespeechChecker.checkHate(msgData.data):
                entityUserDBHandler.updateHateSpeech(msgData.userID)
                output = MessageOutputs(msgData.roomID,msgData.qrcodeID,msgData.userID, "HATE")
                print("produce :", output.__dict__)
                #messageOutputHandler.produce(output)


            # check spam, notify app server, save user data
            if spamChecker.checkSpam(msgData.data):
                entityUserDBHandler.updateSpamming(msgData.userID)
                output = MessageOutputs(msgData.roomID,msgData.qrcodeID,msgData.userID, "SPAM")
                print("produce :", output.__dict__)
                #messageOutputHandler.produce(output)


            if counter.get_counter(msgData.roomID, msgData.qrcodeID) == 10:
                print("update entity room model for: ", msgData.roomID, msgData.qrcodeID)
                counter.set_counter(msgData.roomID, msgData.qrcodeID)

                #messagesDBHandler.readMessagesDataForRoom([msgData.roomID, msgData.qrcodeID])
                # #messageTopicHandler.commit()
                # allData = messagesDBHandler.getCollection('topic-model-messages')
                # print("all data", allData)
                messagesInRoom = messagesDBHandler.readMessagesDataForRoom([msgData.roomID, msgData.qrcodeID])
                #print("Data from MongoDB", messagesInRoom)
                messages: List[str] = messagesInRoom['data']
                messages, ner_labels = text_Preprocessing.process(messages)
                # list of (int, list of (str, float))
                model_topics = topic_modeling.runModel(messages, 10)
                model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics, ner_labels)
               # model_topics = json.dumps(model_topics)
               # print(model_topics)
               # model_topics = encrypt.encrypt(model_topics)
               # ner_labels = encrypt.encrypt(ner_labels)

                # influxDB
                entityRoomDBHandler.updateTopics(msgData.roomID, msgData.qrcodeID, model_topics)

                # pouzitie mongoDB
                #if entityRoomDBHandler.checkEntityRoom([msgData.roomID, msgData.qrcodeID]):
                #    entityRoomDBHandler.updateTopics([msgData.roomID, msgData.qrcodeID], model_topics)
                #else:
                #    roomEntity : RoomEntity = RoomEntity(msgData.roomID, msgData.qrcodeID, model_topics)
                #    entityRoomDBHandler.insertEntityRoom(roomEntity)


def BlRoomAnalyzer():
    roomDataHandler = KafkaHandler.RoomDataAndBlacklistTopicHandler()
    dbHandler = DBHandler()
    influxDBHandler = InfluxDBHandler()
    messagesDBHandler : MessagesDBHandler = dbHandler.getMessagesDBHandler()
    blacklistDBHandler : BlacklistDBHandler = dbHandler.getBlackListDBHandler()
    roomDBHandler : RoomDBHandler = dbHandler.getRoomDBHandler()
    entityUserDBHandler : EntityUserDBHandler = influxDBHandler.getEntityUserDBHandler()
    entityRoomDBHandler : EntityRoomDBHandler = influxDBHandler.getEntityRoomDBHandler()

    while True:
        data = roomDataHandler.consume()
        print("Blacklist and Room analyzer - waiting for data")

        if data:
            print(type(data))
            if isinstance(data, RoomData):
                print("roomData")
                if roomDBHandler.readRoomData(data.qrcodeID):
                    roomDBHandler.updateRoomData(data.qrcodeID, data)
                else:
                    roomDBHandler.insertRoomData(data)
                print("input: ", data.__dict__)

                # read all rooms for qrcodeID
                rooms : List[Dict] = messagesDBHandler.readMessagesDataForAllRooms(data.qrcodeID)
                for room in rooms:
                    roomID = room['roomID']
                    #print("Data from MongoDB", messagesInRoom)
                    messages: List[str] = room['data']

                    # add description and room Name into messages with some ratio 2x for description, 3x for room name
                    for _ in range(2):
                        messages.append(data.description)
                    for _ in range(3):
                        messages.append(data.roomName)

                    messages, ner_labels = text_Preprocessing.process(messages)
                    # list of (int, list of (str, float))
                    model_topics = topic_modeling.runModel(messages, 10)

                    # do some calculation with description and room Name
                    model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics, ner_labels)
                    # print(model_topics)

                    # model_topics = encrypt.encrypt(model_topics)
                    # ner_labels = encrypt.encrypt(ner_labels)

                    # influxDB
                    entityRoomDBHandler.updateTopics(roomID, data.qrcodeID, model_topics)

            elif isinstance(data, BlacklistData):
                print("blacklistData")
                if blacklistDBHandler.readBlacklistData(data.userID):
                    blacklistDBHandler.updateBlacklistData(data.userID, data)
                else:
                    blacklistDBHandler.insertBlacklistData(data)
                print("input: ", data.__dict__)

                blacklistData = blacklistDBHandler.readBlacklistData(data.userID)
                messages, ner_labels = text_Preprocessing.process([blacklistData['notes']])
                # list of (int, list of (str, float))
                model_topics = topic_modeling.runModel(messages, 1)
                model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics, ner_labels)
                entityUserDBHandler.updateTopics(userID=data.userID, topics=model_topics)
            else:
                print("else")
                continue


if __name__ == "__main__":
    p1 = Process(target=BlRoomAnalyzer)
    p1.start()
    p2 = Process(target=messageAnalyzer)
    p2.start()

    p1.join()
    p2.join()