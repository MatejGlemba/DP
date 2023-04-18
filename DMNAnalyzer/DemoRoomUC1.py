from multiprocessing import Process
from time import sleep
from kafka_tools import KafkaHandler
from kafka_tools.serializers import RoomData, MessageData
import csv

from utils.crypto import Crypto

def process1():
    #UC1_1
    handler = KafkaHandler.RoomDataAndBlacklistTopicHandler('localhost:9094')
    with open('demo_data/DemoRoomUC1_1.csv', mode="r") as f:
        csv_reader = csv.reader(f, delimiter='|')
        header_row = next(csv_reader)
            
        # Loop through the remaining rows
        for row in csv_reader:
            # Create an empty dictionary for the current row
            row_dict = {}
            
            # Loop through the values in the current row and add them to the dictionary
            for i in range(len(row)):
                row_dict[header_row[i]] = row[i]
            print(row_dict)
           # sleep(2)
            handler.produce(RoomData(row_dict['qrcodeID'], row_dict['photoPath'], row_dict['description'], row_dict['roomName']))
        handler.flush()
    
    #UC1_2
    messageTopicHandler = KafkaHandler.MessageTopicHandler('localhost:9094')
    with open('demo_data/DemoRoomUC1_2.csv', mode="r") as f:
        csv_reader = csv.reader(f, delimiter='|')
        header_row = next(csv_reader)
            
        # Loop through the remaining rows
        for row in csv_reader:
            # Create an empty dictionary for the current row
            row_dict = {}
            
            # Loop through the values in the current row and add them to the dictionary
            for i in range(len(row)):
                row_dict[header_row[i]] = row[i]
            print(row_dict)
            messageTopicHandler.produce(MessageData(row_dict['roomID'], row_dict['qrcodeID'], row_dict['userID'], row_dict['data']))
        messageTopicHandler.flush()
    
    #UC1_3
    handler = KafkaHandler.RoomDataAndBlacklistTopicHandler('localhost:9094')
    with open('demo_data/DemoRoomUC1_3.csv', mode="r") as f:
        csv_reader = csv.reader(f, delimiter='|')
        header_row = next(csv_reader)
            
        # Loop through the remaining rows
        for row in csv_reader:
            # Create an empty dictionary for the current row
            row_dict = {}
            
            # Loop through the values in the current row and add them to the dictionary
            for i in range(len(row)):
                row_dict[header_row[i]] = row[i]
            print(row_dict)
           # sleep(2)
            handler.produce(RoomData(row_dict['qrcodeID'], row_dict['photoPath'], row_dict['description'], row_dict['roomName']))
        handler.flush()

if __name__ == "__main__":
    process1()