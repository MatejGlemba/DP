from multiprocessing import Process
from typing import Dict, List, Tuple
from kafka_tools import KafkaHandler
from kafka_tools.deserializers import MessageData, BlacklistData, MessageOutputs, RoomData
from utils.crypto import Crypto
from utils.messagesCounter import Counter
from ai_tools import hatespeechChecker, violenceChecker, text_Preprocessing, topic_modeling
from DB_tools.MongoHandler import DBHandler, MessagesDBHandler, BlacklistDBHandler
#from DB_tools.InfluxDBHandler import EntityRoomDBHandler, EntityUserDBHandler, InfluxDBHandler
from DB_tools.PostgreSQLHandler import PostgresDBHandler, EntityRoomDBHandler, EntityUserDBHandler

violenceChecckerOn = True

def messageAnalyzer():
    messageTopicHandler = KafkaHandler.MessageTopicHandler()
    messageOutputHandler = KafkaHandler.MessageOutputsTopicHandler()
    dbHandler = DBHandler()
    #influxDBHandler = InfluxDBHandler()
    postgresDBHandler = PostgresDBHandler()
    messagesDBHandler : MessagesDBHandler = dbHandler.getMessagesDBHandler()
    #entityRoomDBHandler: EntityRoomDBHandler = influxDBHandler.getEntityRoomDBHandler() 
    #entityUserDBHandler: EntityUserDBHandler = influxDBHandler.getEntityUserDBHandler()
    entityRoomDBHandler: EntityRoomDBHandler = postgresDBHandler.getEntityRoomDBHandler() 
    entityUserDBHandler: EntityUserDBHandler = postgresDBHandler.getEntityUserDBHandler()
    counter : Counter = Counter()
    dictOfMessages = {}
    print(type(postgresDBHandler))
    while True:
        msgData : MessageData = messageTopicHandler.consume()
        print("Message analyzer - waiting for data")
        #messageTopicHandler.commit()

        if msgData:
            print("input: ", msgData.__dict__)
            counter.inc_counter(msgData.roomID, msgData.qrcodeID)

            # save incoming data into MongoDB
            if messagesDBHandler.readMessagesDataForRoom([msgData.roomID, msgData.qrcodeID]):
                # msgData = encrypt.encrypt(msgData.data)
                messagesDBHandler.updateMessagesData([msgData.roomID, msgData.qrcodeID], msgData.data)
            else:
                messagesDBHandler.insertMessageData(msgData)
            
            # check hate speech, notify app server, save user data 
            if hatespeechChecker.checkHate(msgData.data):
                entityUserDBHandler.updateHateSpeech(msgData.userID)
                output = MessageOutputs(msgData.roomID,msgData.qrcodeID,msgData.userID, "HATE")
                print("produce :", output.__dict__)
                #messageOutputHandler.produce(output)

            if violenceChecckerOn:
                # save data for violence checking
                if msgData.roomID in dictOfMessages.keys():
                    dictOfMessages[msgData.roomID] += msgData.data
                else:
                    dictOfMessages[msgData.roomID] = msgData.data

                # check violence in messages via openai service
                if counter.get_counter(msgData.roomID, msgData.qrcodeID) % 5 == 0 and msgData.roomID in dictOfMessages.keys():
                    if violenceChecker.check_content(dictOfMessages[msgData.roomID]):
                        print("violence detected")
                        entityRoomDBHandler.updateViolence(msgData.roomID)
                    # refresh data
                    dictOfMessages[msgData.roomID] = ''

            if counter.get_counter(msgData.roomID, msgData.qrcodeID) == 10:
                print("update entity room model for: ", msgData.roomID, msgData.qrcodeID)
                counter.set_counter(msgData.roomID, msgData.qrcodeID)

                messagesInRoom = messagesDBHandler.readMessagesDataForRoom([msgData.roomID, msgData.qrcodeID])
                messages: List[str] = messagesInRoom['data']
                messages, ner_labels = text_Preprocessing.process(messages)
                model_topics = topic_modeling.runModel(messages, 10)
                model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics, ner_labels)

                # influxDB / postgreSQL
                entityRoomDBHandler.updateTopics(msgData.roomID, msgData.qrcodeID, model_topics)
                
                #messageOutputHandler.produce(output)
                # pouzitie mongoDB
                #if entityRoomDBHandler.checkEntityRoom([msgData.roomID, msgData.qrcodeID]):
                #    entityRoomDBHandler.updateTopics([msgData.roomID, msgData.qrcodeID], model_topics)
                #else:
                #    roomEntity : RoomEntity = RoomEntity(msgData.roomID, msgData.qrcodeID, model_topics)
                #    entityRoomDBHandler.insertEntityRoom(roomEntity)


def BlRoomAnalyzer():
    roomDataHandler = KafkaHandler.RoomDataAndBlacklistTopicHandler()
    dbHandler = DBHandler()
    #influxDBHandler = InfluxDBHandler()
    postgresDBHandler = PostgresDBHandler()
    messagesDBHandler : MessagesDBHandler = dbHandler.getMessagesDBHandler()
    blacklistDBHandler : BlacklistDBHandler = dbHandler.getBlackListDBHandler()
   # roomDBHandler : RoomDBHandler = dbHandler.getRoomDBHandler()
   # entityUserDBHandler : EntityUserDBHandler = influxDBHandler.getEntityUserDBHandler()
   # entityRoomDBHandler : EntityRoomDBHandler = influxDBHandler.getEntityRoomDBHandler()
    entityUserDBHandler : EntityUserDBHandler = postgresDBHandler.getEntityUserDBHandler()
    entityRoomDBHandler : EntityRoomDBHandler = postgresDBHandler.getEntityRoomDBHandler()

    print(type(postgresDBHandler))
    while True:
        data = roomDataHandler.consume()
        print("Blacklist and Room analyzer - waiting for data")

        if data:
            if isinstance(data, RoomData):
                print("roomData")

                # update data in mongoDB for topic modeling for all rooms matched by qrcodeID
                if data.description:
                    temp = ""
                    for _ in range(2):
                        temp += data.description
                    # temp = encrypt.encrypt(temp)
                    messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, temp)
                if data.roomName:
                    temp = ""
                    for _ in range(3):
                        temp += data.roomName
                    # temp = encrypt.encrypt(temp)
                    messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, temp)


                print("input: ", data.__dict__)

                # read all rooms for qrcodeID
                rooms : List[Dict] = messagesDBHandler.readMessagesDataForAllRooms(data.qrcodeID)
                for room in rooms:
                    roomID = room['roomID']
                    messages: List[str] = room['data']

                    messages, ner_labels = text_Preprocessing.process(messages)
                    model_topics = topic_modeling.runModel(messages, 10)
                    model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics, ner_labels)

                    # influxDB
                    entityRoomDBHandler.updateTopics(roomID, data.qrcodeID, model_topics)

            elif isinstance(data, BlacklistData):
                print("blacklistData")
                if blacklistDBHandler.readBlacklistData(data.userID):
                    blacklistDBHandler.updateBlacklistData(data.userID, data)
                else:
                    blacklistDBHandler.insertBlacklistData(data)

                blacklistData = blacklistDBHandler.readBlacklistData(data.userID)
                messages, ner_labels = text_Preprocessing.process([blacklistData['notes']])
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