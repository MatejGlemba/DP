from multiprocessing import Process
import re
import time
from typing import Dict, List, Tuple
from kafka_tools import KafkaHandler
from kafka_tools.deserializers import MessageData, UserData, RoomData
from utils.crypto import Crypto
from utils.messagesCounter import Counter
from ai_tools import violence_checker, text_preprocessing, topic_modeling, image_processing
from db_tools.MongoHandler import DBHandler, MessagesDBHandler, BlacklistDBHandler, RoomDBHandler
from db_tools.PostgreSQLHandler import PostgresDBHandler, EntityRoomDBHandler, EntityUserDBHandler
from configparser import ConfigParser

# services
messageTopicHandler : KafkaHandler.MessageTopicHandler = None
roomDataHandler : KafkaHandler.RoomAndUserDataTopicHandler = None
topicHandler : KafkaHandler.TopicHandler = None
dbHandler : DBHandler = None
postgresDBHandler : PostgresDBHandler = None

def readConfig():
    global config
    global messageTopicHandler
    global roomDataHandler
    global dbHandler
    global postgresDBHandler
    global topicHandler

    config = ConfigParser()
    config.read('config.ini')

    postgresDB = config.get('postgresDB', 'db_name')
    postgresUser = config.get('postgresDB', 'user')
    postgresPass = config.get('postgresDB', 'password')
    postgresHost = config.get('postgresDB', 'host')
    postgresPort = config.get('postgresDB', 'port')
    kafkaUri = config.get('kafka', 'kafka_uri')
    mongoUri = config.get('mongoDB', 'mongo_uri')

    # initializers
   # messageTopicHandler = KafkaHandler.MessageTopicHandler(kafkaUri)
    roomDataHandler = KafkaHandler.RoomAndUserDataTopicHandler(kafkaUri)
    topicHandler = KafkaHandler.TopicHandler(kafkaUri)
    dbHandler = DBHandler(mongoUri)
    postgresDBHandler = PostgresDBHandler(postgresDB, postgresUser, postgresPass, postgresHost, postgresPort)

def MessageAnalyzer():
    # configs
    violenceCheckerOn = config.getboolean('analyzer', 'violence_checker_on')
    messagesTreshold = config.getint('analyzer', 'messages_treshold')
    violenceCheckerTreshold = config.getint('analyzer', 'violence_checker_treshold')
    numOfTopics = config.getint('model', 'num_of_topics')
    numOfWordsPerTopic = config.getint('model', 'num_of_words_per_topic')
    numOfPasses = config.getint('model', 'num_of_model_training_passes')
    numOfIterations = config.getint('model', 'num_of_model_training_iterations')
    minTopicProbability = config.getfloat('model', 'min_word_topic_probability')

    # initializers 
    entityRoomDBHandler: EntityRoomDBHandler = postgresDBHandler.getEntityRoomDBHandler()
    messagesDBHandler : MessagesDBHandler = dbHandler.getMessagesDBHandler()
    roomDBHandler : RoomDBHandler = dbHandler.getRoomDBHandler()
    counter : Counter = Counter()

    # violence checker fields
    dictOfMessages = {}
    numOfViolenceChecker = 0
    start_time = time.time()
    reset = False

    # main app
    while True:
        msgData : MessageData = messageTopicHandler.consume()
      #  print("Message analyzer - waiting for data")
     #   messageTopicHandler.commit()
        time.sleep(1)
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
                        categories : Dict = violence_checker.check_content(dictOfMessages[msgData.roomID])
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
                roomData : Dict = roomDBHandler.readRoomData(msgData.qrcodeID)
                
                # decrypt from MongoDB
                decryptedMessages : List[str] = []
                for m in messages:
                    #print("message to be decrypted", m)
                    decryptedM = Crypto.decryptFun(m)
                    decryptedMessages.append(decryptedM)
                
                messages, ner_labels = text_preprocessing.process(sentences=decryptedMessages, addNerLabels=True)
                
                # room name + description + image objects
                topWords = []
                if roomData:
                    if roomData['roomName']:
                        topWords.append(roomData['roomName'])
                    if roomData['description']:
                        topWords.append(roomData['description'])
                    if roomData['imageObjects']:
                        topWords.append(roomData['imageObjects'])
                    topWords = text_preprocessing.process(sentences=topWords, addNerLabels=False)[0]

                messages.append(topWords)
                # model topics for num of topics treshold
                model_topics = topic_modeling.runModel(documents=messages, num_topics=numOfTopics, num_words=numOfWordsPerTopic, passes=numOfPasses, iterations=numOfIterations, minimum_probability=minTopicProbability)
                model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics, ner_labels=ner_labels, topWords=topWords)
           
                # one overall topic for room 
                model_topics_overall = topic_modeling.runModel(documents=messages, num_topics=1, num_words=10, passes=numOfPasses, iterations=numOfIterations, minimum_probability=0.1)
                model_topics_overall : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics_overall, ner_labels=ner_labels, topWords=topWords)

                entityRoomDBHandler.updateTopics(msgData.roomID, msgData.qrcodeID, model_topics, model_topics_overall)  
                
def UserRoomAnalyzer():
    # configs
    topicDescTreshold = config.getint('analyzer', 'topic_description_treshold')
    topicRoomNameTreshold = config.getint('analyzer', 'topic_room_name_treshold')
    numOfTopics = config.getint('model', 'num_of_topics')
    numOfWordsPerTopic = config.getint('model', 'num_of_words_per_topic')
    numOfPasses = config.getint('model', 'num_of_model_training_passes')
    numOfIterations = config.getint('model', 'num_of_model_training_iterations')
    minTopicProbability = config.getfloat('model', 'min_word_topic_probability')
    imageConfThreshold = config.getfloat('model', 'image_confidence_threshold')


    # initializers 
    messagesDBHandler : MessagesDBHandler = dbHandler.getMessagesDBHandler()
    roomDBHandler : RoomDBHandler = dbHandler.getRoomDBHandler()
    blacklistDBHandler : BlacklistDBHandler = dbHandler.getBlackListDBHandler()
    entityUserDBHandler : EntityUserDBHandler = postgresDBHandler.getEntityUserDBHandler()
    entityRoomDBHandler: EntityRoomDBHandler = postgresDBHandler.getEntityRoomDBHandler()
    
    # main App
    while True:
        data = roomDataHandler.consume()
      #  print("Blacklist and Room analyzer - waiting for data")
        time.sleep(1)
        if data:
            if isinstance(data, RoomData):
                # update data in mongoDB for topic modeling for all rooms matched by qrcodeID
                if data.description:
                    # check if description contains html tags
                    if "<p>" in data.description:
                        match = re.search(r'(?<=<p>)(.*?)(?=<)', data.description)
                        if match:
                            matched_text = match.group(1)
                            temp = ""
                            for _ in range(topicDescTreshold):
                                temp += matched_text

                            roomDBHandler.updateRoomDescriptionData(data.qrcodeID, temp)
                            # encrypt data into MongoDB
                            dataEncrypted = Crypto.encryptFun(temp)
                            messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)
                    else:
                        roomDBHandler.updateRoomDescriptionData(data.qrcodeID, data.description)
                        # encrypt data into MongoDB
                        dataEncrypted = Crypto.encryptFun(data.description)
                        messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)

                if data.roomName:
                    temp = ""
                    for _ in range(topicRoomNameTreshold):
                        temp += data.roomName

                    roomDBHandler.updateRoomNameData(data.qrcodeID, temp)
                    # encrypt data into MongoDB
                    dataEncrypted = Crypto.encryptFun(temp)
                    messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)

                if data.photoPath:
                    detectedObjects = image_processing.processImage(imagePath=data.photoPath, conf_threshold=imageConfThreshold)
                    if detectedObjects:
                        temp = ""
                        for _ in range(topicDescTreshold):
                            for detectedObject in detectedObjects:
                                temp += detectedObject

                        roomDBHandler.updateRoomImageObjectsData(data.qrcodeID, temp)
                        # encrypt data into MongoDB
                        dataEncrypted = Crypto.encryptFun(temp)
                        messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)
                
                # read all rooms for qrcodeID
                rooms : List[Dict] = messagesDBHandler.readMessagesDataForAllRooms(data.qrcodeID)
                roomData : Dict = roomDBHandler.readRoomData(data.qrcodeID)

                for room in rooms:
                    roomID = room['roomID']
                    messages: List[str] = room['data']
                    
                    # decrypt from MongoDB
                    decryptedMessages : List[str] = []
                    for m in messages:
                     #   print("message to be decrypted", m)
                        decryptedM = Crypto.decryptFun(m)
                        decryptedMessages.append(decryptedM)
                    
                    messages, ner_labels = text_preprocessing.process(sentences=decryptedMessages, addNerLabels=True)

                    # room name + description + image objects
                    topWords = []
                    if roomData:
                        if roomData['roomName']:
                            topWords.append(roomData['roomName'])
                        if roomData['description']:
                            topWords.append(roomData['description'])
                        if roomData['imageObjects']:
                            topWords.append(roomData['imageObjects'])
                        topWords = text_preprocessing.process(sentences=topWords, addNerLabels=False)[0]

                    messages.append(topWords)
                    # model topics for num of topics treshold
                    model_topics = topic_modeling.runModel(documents=messages, num_topics=numOfTopics, num_words=numOfWordsPerTopic, passes=numOfPasses, iterations=numOfIterations, minimum_probability=minTopicProbability)
                    model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics, ner_labels=ner_labels, topWords=topWords)

                    # one overall topic for room
                    model_topics_overall = topic_modeling.runModel(documents=messages, num_topics=1, num_words=10, passes=numOfPasses, iterations=numOfIterations, minimum_probability=0.1)
                    model_topics_overall : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics_overall, ner_labels=ner_labels, topWords=topWords)
                
                    entityRoomDBHandler.updateTopics(roomID, data.qrcodeID, model_topics, model_topics_overall)
            elif isinstance(data, UserData):
                if data.notes == "HATE":
                    entityUserDBHandler.updateHateSpeech(data.userID)
                else:
                    if blacklistDBHandler.readUserData(data.userID):
                        msgDataEncrypted = Crypto.encryptFun(data.notes)
                        blacklistDBHandler.updateUserData(data.userID, msgDataEncrypted)
                    else:
                        msgDataEncrypted = Crypto.encryptFun(data.notes)
                        blacklistDBHandler.insertUserData(data.userID, msgDataEncrypted)

                    userData : List[str] = blacklistDBHandler.readUserData(data.userID)
                    notes: List[str] = userData['data']

                    # decrypt from MongoDB
                    decryptedMessages : List[str] = []
                    for note in notes:
                        decryptedM = Crypto.decryptFun(note)
                        decryptedMessages.append(decryptedM)

                    messages, ner_labels = text_preprocessing.process(sentences=decryptedMessages, addNerLabels=True)
                    model_topics = topic_modeling.runModel(documents=messages, num_topics=1, num_words=10, passes=numOfPasses, iterations=numOfIterations, minimum_probability=0.1)
                    model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics, ner_labels=ner_labels, topWords=None)
                    entityUserDBHandler.updateTopics(userID=data.userID, topics=model_topics)
            else:
                pass

def analyzer():

    # configs
    topicDescTreshold = config.getint('analyzer', 'topic_description_treshold')
    topicRoomNameTreshold = config.getint('analyzer', 'topic_room_name_treshold')
    imageConfThreshold = config.getfloat('model', 'image_confidence_threshold')


    # initializers 
    blacklistDBHandler : BlacklistDBHandler = dbHandler.getBlackListDBHandler()
    entityUserDBHandler : EntityUserDBHandler = postgresDBHandler.getEntityUserDBHandler()

    # configs
    violenceCheckerOn = config.getboolean('analyzer', 'violence_checker_on')
    messagesTreshold = config.getint('analyzer', 'messages_treshold')
    violenceCheckerTreshold = config.getint('analyzer', 'violence_checker_treshold')
    numOfTopics = config.getint('model', 'num_of_topics')
    numOfWordsPerTopic = config.getint('model', 'num_of_words_per_topic')
    numOfPasses = config.getint('model', 'num_of_model_training_passes')
    numOfIterations = config.getint('model', 'num_of_model_training_iterations')
    minTopicProbability = config.getfloat('model', 'min_word_topic_probability')

    # initializers 
    entityRoomDBHandler: EntityRoomDBHandler = postgresDBHandler.getEntityRoomDBHandler()
    messagesDBHandler : MessagesDBHandler = dbHandler.getMessagesDBHandler()
    roomDBHandler : RoomDBHandler = dbHandler.getRoomDBHandler()
    counter : Counter = Counter()

    # violence checker fields
    dictOfMessages = {}
    numOfViolenceChecker = 0
    start_time = time.time()
    reset = False

    # main App
    while True:
        data = topicHandler.consume()
        print("analyzer - waiting for data", flush=True)
        time.sleep(1)
        if data:
            if isinstance(data, MessageData):
                msgData : MessageData = data 
                print("input: ", msgData.__dict__, flush=True)
                counter.inc_counter(msgData.roomID, msgData.qrcodeID)

                # save incoming data into MongoDB
                if messagesDBHandler.readMessagesDataForRoom(msgData.roomID, msgData.qrcodeID):
                    msgDataEncrypted = Crypto.encryptFun(msgData.data)
                    messagesDBHandler.updateMessagesData(msgData.roomID, msgData.qrcodeID, msgDataEncrypted)
                else:
                    msgDataEncrypted = Crypto.encryptFun(msgData.data)
                    messagesDBHandler.insertMessageData(msgData.roomID, msgData.qrcodeID, msgDataEncrypted)
                
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
                            categories : Dict = violence_checker.check_content(dictOfMessages[msgData.roomID])
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
                    print("update entity room model for: ", msgData.roomID, msgData.qrcodeID, flush=True)
                    counter.set_counter(msgData.roomID, msgData.qrcodeID)

                    messagesInRoom = messagesDBHandler.readMessagesDataForRoom(msgData.roomID, msgData.qrcodeID)
                    messages: List[str] = messagesInRoom['data']
                    roomData : Dict = roomDBHandler.readRoomData(msgData.qrcodeID)
                    
                    # decrypt from MongoDB
                    decryptedMessages : List[str] = []
                    for m in messages:
                        #print("message to be decrypted", m)
                        decryptedM = Crypto.decryptFun(m)
                        decryptedMessages.append(decryptedM)
                    
                    messages, ner_labels = text_preprocessing.process(sentences=decryptedMessages, addNerLabels=True)
                    
                    # room name + description + image objects
                    topWords = []
                    if roomData:
                        if roomData['roomName']:
                            topWords.append(roomData['roomName'])
                        if roomData['description']:
                            topWords.append(roomData['description'])
                        if roomData['imageObjects']:
                            topWords.append(roomData['imageObjects'])
                        topWords = text_preprocessing.process(sentences=topWords, addNerLabels=False)[0]

                    messages.append(topWords)
                    # model topics for num of topics treshold
                    model_topics = topic_modeling.runModel(documents=messages, num_topics=numOfTopics, num_words=numOfWordsPerTopic, passes=numOfPasses, iterations=numOfIterations, minimum_probability=minTopicProbability)
                    model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics, ner_labels=ner_labels, topWords=topWords)
            
                    # one overall topic for room 
                    model_topics_overall = topic_modeling.runModel(documents=messages, num_topics=1, num_words=10, passes=numOfPasses, iterations=numOfIterations, minimum_probability=0.1)
                    model_topics_overall : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics_overall, ner_labels=ner_labels, topWords=topWords)

                    entityRoomDBHandler.updateTopics(msgData.roomID, msgData.qrcodeID, model_topics, model_topics_overall)  
            elif isinstance(data, RoomData):
                # update data in mongoDB for topic modeling for all rooms matched by qrcodeID
                data : RoomData = data 
                print("input: ", data.__dict__, flush=True)
                if data.description:
                    # check if description contains html tags
                    if "<p>" in data.description:
                        match = re.search(r'(?<=<p>)(.*?)(?=<)', data.description)
                        if match:
                            matched_text = match.group(1)
                            temp = ""
                            for _ in range(topicDescTreshold):
                                temp += matched_text

                            roomDBHandler.updateRoomDescriptionData(data.qrcodeID, temp)
                            # encrypt data into MongoDB
                            dataEncrypted = Crypto.encryptFun(temp)
                            messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)
                    else:
                        roomDBHandler.updateRoomDescriptionData(data.qrcodeID, data.description)
                        # encrypt data into MongoDB
                        dataEncrypted = Crypto.encryptFun(data.description)
                        messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)

                if data.roomName:
                    temp = ""
                    for _ in range(topicRoomNameTreshold):
                        temp += data.roomName

                    roomDBHandler.updateRoomNameData(data.qrcodeID, temp)
                    # encrypt data into MongoDB
                    dataEncrypted = Crypto.encryptFun(temp)
                    messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)

                if data.photoPath:
                    detectedObjects = image_processing.processImage(imagePath=data.photoPath, conf_threshold=imageConfThreshold)
                    if detectedObjects:
                        temp = ""
                        for _ in range(topicDescTreshold):
                            for detectedObject in detectedObjects:
                                temp += detectedObject

                        roomDBHandler.updateRoomImageObjectsData(data.qrcodeID, temp)
                        # encrypt data into MongoDB
                        dataEncrypted = Crypto.encryptFun(temp)
                        messagesDBHandler.updateMessagesDataForAllRooms(data.qrcodeID, dataEncrypted)
                
                # read all rooms for qrcodeID
                rooms : List[Dict] = messagesDBHandler.readMessagesDataForAllRooms(data.qrcodeID)
                roomData : Dict = roomDBHandler.readRoomData(data.qrcodeID)

                for room in rooms:
                    roomID = room['roomID']
                    messages: List[str] = room['data']
                    
                    # decrypt from MongoDB
                    decryptedMessages : List[str] = []
                    for m in messages:
                     #   print("message to be decrypted", m)
                        decryptedM = Crypto.decryptFun(m)
                        decryptedMessages.append(decryptedM)
                    
                    messages, ner_labels = text_preprocessing.process(sentences=decryptedMessages, addNerLabels=True)

                    # room name + description + image objects
                    topWords = []
                    if roomData:
                        if roomData['roomName']:
                            topWords.append(roomData['roomName'])
                        if roomData['description']:
                            topWords.append(roomData['description'])
                        if roomData['imageObjects']:
                            topWords.append(roomData['imageObjects'])
                        topWords = text_preprocessing.process(sentences=topWords, addNerLabels=False)[0]

                    messages.append(topWords)
                    # model topics for num of topics treshold
                    model_topics = topic_modeling.runModel(documents=messages, num_topics=numOfTopics, num_words=numOfWordsPerTopic, passes=numOfPasses, iterations=numOfIterations, minimum_probability=minTopicProbability)
                    model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics, ner_labels=ner_labels, topWords=topWords)

                    # one overall topic for room
                    model_topics_overall = topic_modeling.runModel(documents=messages, num_topics=1, num_words=10, passes=numOfPasses, iterations=numOfIterations, minimum_probability=0.1)
                    model_topics_overall : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics_overall, ner_labels=ner_labels, topWords=topWords)
                
                    entityRoomDBHandler.updateTopics(roomID, data.qrcodeID, model_topics, model_topics_overall)
            elif isinstance(data, UserData):
                data : UserData = data 
                print("input: ", data.__dict__, flush=True)
                if data.notes == "HATE":
                    entityUserDBHandler.updateHateSpeech(data.userID)
                else:
                    if blacklistDBHandler.readUserData(data.userID):
                        msgDataEncrypted = Crypto.encryptFun(data.notes)
                        blacklistDBHandler.updateUserData(data.userID, msgDataEncrypted)
                    else:
                        msgDataEncrypted = Crypto.encryptFun(data.notes)
                        blacklistDBHandler.insertUserData(data.userID, msgDataEncrypted)

                    userData : List[str] = blacklistDBHandler.readUserData(data.userID)
                    notes: List[str] = userData['data']

                    # decrypt from MongoDB
                    decryptedMessages : List[str] = []
                    for note in notes:
                        decryptedM = Crypto.decryptFun(note)
                        decryptedMessages.append(decryptedM)

                    messages, ner_labels = text_preprocessing.process(sentences=decryptedMessages, addNerLabels=True)
                    model_topics = topic_modeling.runModel(documents=messages, num_topics=1, num_words=10, passes=numOfPasses, iterations=numOfIterations, minimum_probability=0.1)
                    model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics=model_topics, ner_labels=ner_labels, topWords=None)
                    entityUserDBHandler.updateTopics(userID=data.userID, topics=model_topics)
            else:
                pass


if __name__ == "__main__":
    readConfig()
    analyzer()
    # p1 = Process(target=UserRoomAnalyzer)
    # p1.start()
    # p2 = Process(target=MessageAnalyzer)
    # p2.start()
    # p1.join()
    # p2.join()