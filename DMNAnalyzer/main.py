from multiprocessing import Process
import re
import time
from typing import Dict, List, Tuple
from kafka_tools import KafkaHandler
from kafka_tools.deserializers import MessageData, BlacklistData, MessageOutputs, RoomData
from utils.crypto import Crypto
from utils.messagesCounter import Counter
from ai_tools import hatespeechChecker, violenceChecker, text_Preprocessing, topic_modeling
from DB_tools.MongoHandler import DBHandler, MessagesDBHandler, BlacklistDBHandler
from DB_tools.PostgreSQLHandler import PostgresDBHandler, EntityRoomDBHandler, EntityUserDBHandler
from configparser import ConfigParser

def readConfig():
  global config
  config = ConfigParser()
  config.read('config.ini')

def messageAnalyzer():
    # configs
    violenceCheckerOn = config.getboolean('analyzer', 'violence_checker_on')
    messagesTreshold = config.getint('analyzer', 'messages_treshold')
    violenceCheckerTreshold = config.getint('analyzer', 'violence_checker_treshold')
    numOfTopics = config.getint('analyzer', 'num_of_topics')
    postgresDB = config.get('postgresDB', 'db_name')
    postgresUser = config.get('postgresDB', 'user')
    postgresPass = config.get('postgresDB', 'password')
    postgresHost = config.get('postgresDB', 'host')
    postgresPort = config.get('postgresDB', 'port')
    kafkaUri = config.get('kafka', 'kafka_uri')
    mongoUri = config.get('mongoDB', 'mongo_uri')

    # initializers
    messageTopicHandler = KafkaHandler.MessageTopicHandler(kafkaUri)
    messageOutputHandler = KafkaHandler.MessageOutputsTopicHandler(kafkaUri)
    dbHandler = DBHandler(mongoUri)
    postgresDBHandler = PostgresDBHandler(postgresDB, postgresUser, postgresPass, postgresHost, postgresPort)
    messagesDBHandler : MessagesDBHandler = dbHandler.getMessagesDBHandler()
    entityRoomDBHandler: EntityRoomDBHandler = postgresDBHandler.getEntityRoomDBHandler() 
    entityUserDBHandler: EntityUserDBHandler = postgresDBHandler.getEntityUserDBHandler()
    counter : Counter = Counter()

    # violence checker fields
    dictOfMessages = {}
    numOfViolenceChecker = 0
    start_time = time.time()
    reset = False

    # main app
    while True:
        msgData : MessageData = messageTopicHandler.consume()
        print("Message analyzer - waiting for data")
        messageTopicHandler.commit()
        if msgData:
            print("input: ", msgData.__dict__)
            counter.inc_counter(msgData.roomID, msgData.qrcodeID)

            # save incoming data into MongoDB
            if messagesDBHandler.readMessagesDataForRoom(msgData.roomID, msgData.qrcodeID):
                msgDataEncrypted = Crypto.encryptFun(msgData.data)
                messagesDBHandler.updateMessagesData(msgData.roomID, msgData.qrcodeID, msgDataEncrypted)
            else:
                msgDataEncrypted = Crypto.encryptFun(msgData.data)
                messagesDBHandler.insertMessageData(msgData.roomID, msgData.qrcodeID, msgDataEncrypted)
            
            # check hate speech, notify app server, save user data 
            if hatespeechChecker.checkHate(msgData.data):
                entityUserDBHandler.updateHateSpeech(msgData.userID)
                output = MessageOutputs(msgData.roomID,msgData.qrcodeID,msgData.userID, "HATE")
                print("produce :", output.__dict__)
                #messageOutputHandler.produce(output)

            # check violation
            if violenceCheckerOn:
                # save data for violence checking
                if msgData.roomID in dictOfMessages.keys():
                    dictOfMessages[msgData.roomID] += msgData.data
                else:
                    dictOfMessages[msgData.roomID] = msgData.data

                # check violence in messages via openai service
                if counter.get_counter(msgData.roomID, msgData.qrcodeID) % violenceCheckerTreshold == 0 and msgData.roomID in dictOfMessages.keys():
                    # Check if 60 seconds have passed since last reset
                    if reset and time.time() - start_time < 60:
                        print("Waiting for reset")
                    else:
                        # Execute the desired logic here
                        categories : Dict = violenceChecker.check_content(dictOfMessages[msgData.roomID])
                        if categories:
                            print("Violence detected")
                            entityRoomDBHandler.updateViolence(msgData.roomID, categories)

                        # Refresh data and update counter
                        dictOfMessages[msgData.roomID] = ''
                        numOfViolenceChecker += 1
                        reset = False

                        # Check if maximum allowed executions have been reached
                        if numOfViolenceChecker >= 3:
                            # Reset counter and start timer for 60-second reset period
                            start_time = time.time()
                            reset = True
                            numOfViolenceChecker = 0
            
            # build topics
            if counter.get_counter(msgData.roomID, msgData.qrcodeID) == messagesTreshold:
                print("update entity room model for: ", msgData.roomID, msgData.qrcodeID)
                counter.set_counter(msgData.roomID, msgData.qrcodeID)

                messagesInRoom = messagesDBHandler.readMessagesDataForRoom(msgData.roomID, msgData.qrcodeID)
                messages: List[str] = messagesInRoom['data']
                
                # decrypt from MongoDB
                decryptedMessages : List[str] = []
                for m in messages:
                    #print("message to be decrypted", m)
                    decryptedM = Crypto.decryptFun(m)
                    decryptedMessages.append(decryptedM)
                
                messages, ner_labels = text_Preprocessing.process(decryptedMessages)
                
                # model topics for num of topics treshold
                model_topics = topic_modeling.runModel(messages, numOfTopics)
                model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics, ner_labels)
           
                # one overall topic for room 
                model_topics_overall = topic_modeling.runModel(messages, 1)
                model_topics_overall : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics_overall, ner_labels)

                entityRoomDBHandler.updateTopics(msgData.roomID, msgData.qrcodeID, model_topics, model_topics_overall)  
                
def BlRoomAnalyzer():
    # configs
    topicDescTreshold = config.getint('analyzer', 'topic_description_treshold')
    topicRoomNameTreshold = config.getint('analyzer', 'topic_room_name_treshold')
    numOfTopics = config.getint('analyzer', 'num_of_topics')
    postgresDB = config.get('postgresDB', 'db_name')
    postgresUser = config.get('postgresDB', 'user')
    postgresPass = config.get('postgresDB', 'password')
    postgresHost = config.get('postgresDB', 'host')
    postgresPort = config.get('postgresDB', 'port')
    kafkaUri = config.get('kafka', 'kafka_uri')
    mongoUri = config.get('mongoDB', 'mongo_uri')

    # initializers
    roomDataHandler = KafkaHandler.RoomDataAndBlacklistTopicHandler(kafkaUri)
    dbHandler = DBHandler(mongoUri)
    postgresDBHandler = PostgresDBHandler(postgresDB, postgresUser, postgresPass, postgresHost, postgresPort)
    messagesDBHandler : MessagesDBHandler = dbHandler.getMessagesDBHandler()
    blacklistDBHandler : BlacklistDBHandler = dbHandler.getBlackListDBHandler()
    entityUserDBHandler : EntityUserDBHandler = postgresDBHandler.getEntityUserDBHandler()
    entityRoomDBHandler : EntityRoomDBHandler = postgresDBHandler.getEntityRoomDBHandler()

    # main App
    while True:
        data = roomDataHandler.consume()
        print("Blacklist and Room analyzer - waiting for data")

        if data:
            if isinstance(data, RoomData):
                # update data in mongoDB for topic modeling for all rooms matched by qrcodeID
                if data.description:
                    # check if description contains html tags
                    if "<p>" in data.description:
                        match = re.search(r"<p>(.*)<span", data.description)
                        if match:
                            matched_text = match.group(1)
                        temp = ""
                        for _ in range(topicDescTreshold):
                            temp += matched_text
                    else:
                        temp = ""
                        for _ in range(topicDescTreshold):
                            temp += data.description

                    # encrypt data into MongoDB
                    dataEncrypted = Crypto.encryptFun(temp)
                    messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)

                if data.roomName:
                    temp = ""
                    for _ in range(topicRoomNameTreshold):
                        temp += data.roomName

                    # encrypt data into MongoDB
                    dataEncrypted = Crypto.encryptFun(temp)
                    messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)

                # read all rooms for qrcodeID
                rooms : List[Dict] = messagesDBHandler.readMessagesDataForAllRooms(data.qrcodeID)
                for room in rooms:
                    roomID = room['roomID']
                    messages: List[str] = room['data']
                    
                    # decrypt from MongoDB
                    decryptedMessages : List[str] = []
                    for m in messages:
                     #   print("message to be decrypted", m)
                        decryptedM = Crypto.decryptFun(m)
                        decryptedMessages.append(decryptedM)
                    
                    messages, ner_labels = text_Preprocessing.process(decryptedMessages)

                    # model topics for num of topics treshold
                    model_topics = topic_modeling.runModel(messages, numOfTopics)
                    model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics, ner_labels)

                    # one overall topic for room
                    model_topics_overall = topic_modeling.runModel(messages, 1)
                    model_topics_overall : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics_overall, ner_labels)
                
                    entityRoomDBHandler.updateTopics(roomID, data.qrcodeID, model_topics, model_topics_overall)

            elif isinstance(data, BlacklistData):
                if blacklistDBHandler.readBlacklistData(data.userID):
                    msgDataEncrypted = Crypto.encryptFun(data.notes)
                    blacklistDBHandler.updateBlacklistData(data.userID, msgDataEncrypted)
                else:
                    msgDataEncrypted = Crypto.encryptFun(data.notes)
                    blacklistDBHandler.insertBlacklistData(data.userID, msgDataEncrypted)

                blacklistData : List[str] = blacklistDBHandler.readBlacklistData(data.userID)
                notes: List[str] = blacklistData['data']

                # decrypt from MongoDB
                decryptedMessages : List[str] = []
                for note in notes:
                    decryptedM = Crypto.decryptFun(note)
                    decryptedMessages.append(decryptedM)

                messages, ner_labels = text_Preprocessing.process(decryptedMessages)
                model_topics = topic_modeling.runModel(messages, 1)
                model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics, ner_labels)
                entityUserDBHandler.updateTopics(userID=data.userID, topics=model_topics)

            else:
                pass


if __name__ == "__main__":
    readConfig()
    p1 = Process(target=BlRoomAnalyzer)
    p1.start()
    p2 = Process(target=messageAnalyzer)
    p2.start()
    p1.join()
    p2.join()