from multiprocessing import Process
from kafka_tools import KafkaHandler
from kafka_tools.deserializers import MessageData, BlacklistData, MessageOutputs, RoomData
from utils import decrypt, encrypt
from ai_tools import hatespeechChecker, spamChecker, text_Preprocessing, topic_modeling
from mongoDB_tools.dbclient import MongoDBHandler
import csv

def runAnalysisOnMessages():
    messageTopicHandler = KafkaHandler.MessageTopicHandler()
    messageOutputsHanlder = KafkaHandler.MessageOutputsTopicHandler()


    incomingMessages = 0
    while True:
        # receive data 
        msgData : MessageData = messageTopicHandler.consume()

        # decrypt data 
        msg : str = decrypt.decrypt(msgData.data)
        # check hatespeech
        hate = hatespeechChecker.checkHate(msg)
        if hate:
            messageOutputsHanlder.produce(object=MessageOutputs(msgData.roomID, msgData.qrcodeID, msgData.userID, "hate"))
            MongoDBHandler.updateData("entity-model-user", "userId", msgData.userID, "hate-speech", hate)
        # check spam
        spam = spamChecker.checkSpam(msg)
        if spam:
            messageOutputsHanlder.produce(object=MessageOutputs(msgData.roomID, msgData.qrcodeID, msgData.userID, "spam"))
            MongoDBHandler.updateData("entity-model-user", "userId", msgData.userID, "spamming", hate)
        
        # save to DB
        msg : str = encrypt.encrypt(msg)
        MongoDBHandler.updateData("topic-model-messages", ["roomID", "qrcodeID"], [msgData.roomID, msgData.qrcodeID], "messages", msg)
        
        incomingMessages += 1
        # check limit of new messages -> process text modeling
        if incomingMessages > 15:
            incomingMessages = 0
            messages = MongoDBHandler.readData("topic-model-messages", ["roomID", "qrcodeID"], [msgData.roomID, msgData.qrcodeID])
            messages = decrypt.decrypt(messages)
            messages, ner_labels = text_Preprocessing.process(messages)
            model_topics = topic_modeling.runModel(messages)
            model_topics = topic_modeling.updatePercentage(model_topics, ner_labels)
            model_topics = encrypt.encrypt(model_topics)
            ner_labels = encrypt.encrypt(ner_labels)
            MongoDBHandler.updateData("topic-model-outputs", ["roomID", "qrcodeID"], [msgData.roomID, msgData.qrcodeID], ["vectors", "ner_labels"], [model_topics, ner_labels])

def runAnalysisOnBlacklist():
    blacklistTopicHandler = KafkaHandler.BlacklistTopicHandler()

    while True:
        # receive data 
        blkData : BlacklistData = blacklistTopicHandler.consume()
        # decrypt data 
        notes : str = decrypt.decrypt(blkData.notes)
        notes, ner_labels = text_Preprocessing.process(notes)
        model_topics = topic_modeling.runModel(notes)
        model_topics = topic_modeling.updatePercentage(model_topics, ner_labels)
        model_topics = encrypt.encrypt(model_topics)
        MongoDBHandler.updateData("entity-model-user", "userId", blkData.userID, "topics", model_topics)

def runAnalysisOnRoom():
    roomDataTopicHandler = KafkaHandler.RoomDataTopicHandler()

    while True:
        # receive data 
        roomData : RoomData = roomDataTopicHandler.consume()
        # decrypt data 
        image : bytearray = decrypt.decrypt(roomData.image)
        image_topics = topic_modeling.runImageModel(image)
        image_topics = topic_modeling.updatePercentage(roomData.qrcodeID, image_topics)
        # TODO : resolve calculation of percetange in model with image topics

def serviceStarter():
    p1 = Process(target=runAnalysisOnMessages)
    p1.start()
    p2 = Process(target=runAnalysisOnBlacklist)
    p2.start()
    p3 = Process(target=runAnalysisOnRoom)
    p3.start()

    p1.join()
    p2.join()
    p3.join()


def dataDumper():
    messageTopicHandler = KafkaHandler.MessageTopicHandler()
    messageOutputHandler = KafkaHandler.MessageOutputsTopicHandler()
    fieldsNames = ['roomID', 'qrcodeID', 'userID', 'data']
    with open('results/messages.csv', 'w', encoding='UTF8', newline='') as f:
        csvWriter = csv.DictWriter(f, fieldnames=fieldsNames)
        csvWriter.writeheader()
        while True:
            msgData : MessageData = messageTopicHandler.consume()
            print("temp")
           # print(type(msgData))
            #print(type(msgData.__dict__))
            if msgData:
                print("input: ", msgData.__dict__)
                csvWriter.writerow(msgData.__dict__)
                output = MessageOutputs(msgData.roomID,msgData.qrcodeID,msgData.userID, "HATE")
                print("produce :", output.__dict__)
                messageOutputHandler.produce(output)
                    
def BLDumper():
    blackListHandler = KafkaHandler.BlacklistTopicHandler()
    fieldsNames = ['userID', 'notes']
    with open('results/bldata.csv', 'w', encoding='UTF8', newline='') as f:
        csvWriter = csv.DictWriter(f, fieldnames=fieldsNames)
        csvWriter.writeheader()
        while True:
            blData : BlacklistData = blackListHandler.consume()
            print("temp BL")
           # print(type(msgData))
            #print(type(msgData.__dict__))
            if blData:
                print("input: ", blData.__dict__)
                csvWriter.writerow(blData.__dict__)
                
def RoomDumper():
    roomDataHandler = KafkaHandler.RoomDataTopicHandler()
    fieldsNames = ['qrcodeID', 'photoPath', 'description', 'roomName']
    with open('results/roomData.csv', 'w', encoding='UTF8', newline='') as f:
        csvWriter = csv.DictWriter(f, fieldnames=fieldsNames)
        csvWriter.writeheader()
        while True:
            roomData : RoomData = roomDataHandler.consume()
            print("temp Room")
           # print(type(msgData))
            #print(type(msgData.__dict__))
            if roomData:
                print("input: ", roomData.__dict__)
                csvWriter.writerow(roomData.__dict__)

if __name__ == "__main__":
    #serviceStarter()
    p1 = Process(target=BLDumper)
    p1.start()
    p2 = Process(target=dataDumper)
    p2.start()
    p3 = Process(target=RoomDumper)
    p3.start()

    p1.join()
    p2.join()
    p3.join()

    #dataDumper()