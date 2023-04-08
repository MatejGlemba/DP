import json
from multiprocessing import Process
from time import sleep
from kafka_tools import KafkaHandler
from kafka_tools.serializers import BlacklistData, RoomData, MessageData
import csv


KAFKA_BOOTSTRAP_SERVERS_CONS = "localhost:9094"
KAFKA_TOPIC_MESSAGE_DATA = "MESSAGE_DATA"
KAFKA_TOPIC_ROOM_DATA = "ROOM_DATA"
KAFKA_TOPIC_BL_DATA = "BLACKLIST_DATA"

def blacklistInput():
    blacklistDataTopicHandler = KafkaHandler.RoomDataAndBlacklistTopicHandler()
    with open('results/bldataInput.csv', mode="r") as f:
        csv_reader = csv.reader(f)
        header_row = next(csv_reader)
            
        # Loop through the remaining rows
        for row in csv_reader:
            # Create an empty dictionary for the current row
            row_dict = {}
            
            # Loop through the values in the current row and add them to the dictionary
            for i in range(len(row)):
                row_dict[header_row[i]] = row[i]
            print(row_dict)
            sleep(2)
            blacklistDataTopicHandler.produce(BlacklistData(row_dict['userID'], row_dict['notes']))
        blacklistDataTopicHandler.flush()

def roomInput():
    roomDataTopicHandler = KafkaHandler.RoomDataTopicHandler()
    with open('results/roomDataInput.csv', mode="r") as f:
        csv_reader = csv.reader(f)
        header_row = next(csv_reader)
            
        # Loop through the remaining rows
        for row in csv_reader:
            # Create an empty dictionary for the current row
            row_dict = {}
            
            # Loop through the values in the current row and add them to the dictionary
            for i in range(len(row)):
                row_dict[header_row[i]] = row[i]
            print(row_dict)
            sleep(2)
            roomDataTopicHandler.produce(RoomData(row_dict['qrcodeID'], row_dict['photoPath'], row_dict['description'], row_dict['roomName']))
        roomDataTopicHandler.flush()

def messageInput():
    messageTopicHandler = KafkaHandler.MessageTopicHandler()
    with open('results/messagesInput.csv', mode="r") as f:
        csv_reader = csv.reader(f)
        header_row = next(csv_reader)
            
        # Loop through the remaining rows
        for row in csv_reader:
            # Create an empty dictionary for the current row
            row_dict = {}
            
            # Loop through the values in the current row and add them to the dictionary
            for i in range(len(row)):
                row_dict[header_row[i]] = row[i]
            print(row_dict)
            #sleep(2)
            messageTopicHandler.produce(MessageData(row_dict['roomID'], row_dict['qrcodeID'], row_dict['userID'], row_dict['data']))
        messageTopicHandler.flush()

def roomBlinput():
    handler = KafkaHandler.RoomDataAndBlacklistTopicHandler()
    with open('results/roomDataInput.csv', mode="r") as f:
        csv_reader = csv.reader(f)
        header_row = next(csv_reader)
            
        # Loop through the remaining rows
        for row in csv_reader:
            # Create an empty dictionary for the current row
            row_dict = {}
            
            # Loop through the values in the current row and add them to the dictionary
            for i in range(len(row)):
                row_dict[header_row[i]] = row[i]
            print(row_dict)
            sleep(2)
            handler.produce(RoomData(row_dict['qrcodeID'], row_dict['photoPath'], row_dict['description'], row_dict['roomName']))
        handler.flush()

if __name__ == "__main__":
    #serviceStarter()
    p1 = Process(target=roomBlinput)
    p1.start()
    p2 = Process(target=blacklistInput)
    p2.start()
    p1.join()
    p2.join()
