import json
from time import sleep
from kafka_tools import KafkaHandler
from kafka_tools.serializers import BlacklistData, RoomData, MessageData
import csv


KAFKA_BOOTSTRAP_SERVERS_CONS = "localhost:9094"
KAFKA_TOPIC_MESSAGE_DATA = "MESSAGE_DATA"
KAFKA_TOPIC_ROOM_DATA = "ROOM_DATA"
KAFKA_TOPIC_BL_DATA = "BLACKLIST_DATA"

blacklistDataTopicHandler = KafkaHandler.BlacklistTopicHandler()
with open('bldataInput.csv', mode="r") as f:
    csv_reader = csv.reader(f)
    header_row = next(csv_reader)
        
    # Loop through the remaining rows
    for row in csv_reader:
        # Create an empty dictionary for the current row
        row_dict = {}
        
        # Loop through the values in the current row and add them to the dictionary
        for i in range(len(row)):
            row_dict[header_row[i]] = row[i]
        
        blacklistDataTopicHandler.produce(BlacklistData(row_dict['roomID'], row_dict['notes']))

blacklistDataTopicHandler.close()
roomDataTopicHandler = KafkaHandler.RoomDataTopicHandler()
with open('roomDataInput.csv', mode="r") as f:
    csv_reader = csv.reader(f)
    header_row = next(csv_reader)
        
    # Loop through the remaining rows
    for row in csv_reader:
        # Create an empty dictionary for the current row
        row_dict = {}
        
        # Loop through the values in the current row and add them to the dictionary
        for i in range(len(row)):
            row_dict[header_row[i]] = row[i]
        
        roomDataTopicHandler.produce(RoomData(row_dict['qrcodeID'], row_dict['photoPath'], row_dict['description'], row_dict['roomName']))

roomDataTopicHandler.close()

messageTopicHandler = KafkaHandler.MessageTopicHandler()
with open('messagesInput.csv', mode="r") as f:
    csv_reader = csv.reader(f)
    header_row = next(csv_reader)
        
    # Loop through the remaining rows
    for row in csv_reader:
        # Create an empty dictionary for the current row
        row_dict = {}
        
        # Loop through the values in the current row and add them to the dictionary
        for i in range(len(row)):
            row_dict[header_row[i]] = row[i]

        messageTopicHandler.produce(MessageData(row_dict['roomID'], row_dict['qrcodeID'], row_dict['userID'], row_dict['data']))

messageTopicHandler.close()

