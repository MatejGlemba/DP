from multiprocessing import Process
from time import sleep
from kafka_tools import KafkaHandler
from kafka_tools.serializers import UserData, MessageData
import csv

def process1():
    #UC1_1
    messageTopicHandler = KafkaHandler.MessageTopicHandler('localhost:9094')
    with open('demo_data/DemoUserUC1_1.csv', mode="r") as f:
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
            #sleep(2)
            messageTopicHandler.produce(MessageData(row_dict['roomID'], row_dict['qrcodeID'], row_dict['userID'], row_dict['data']))
        messageTopicHandler.flush()

    #UC1_2
    UserDataTopicHandler = KafkaHandler.RoomAndUserDataTopicHandler('localhost:9094')
    with open('demo_data/DemoUserUC1_2.csv', mode="r") as f:
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
            sleep(2)
            UserDataTopicHandler.produce(UserData(row_dict['userID'], row_dict['notes']))
        UserDataTopicHandler.flush()

    #UC1_3
    messageTopicHandler = KafkaHandler.MessageTopicHandler('localhost:9094')
    with open('demo_data/DemoUserUC1_3.csv', mode="r") as f:
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
            #sleep(2)
            messageTopicHandler.produce(MessageData(row_dict['roomID'], row_dict['qrcodeID'], row_dict['userID'], row_dict['data']))
        messageTopicHandler.flush()

if __name__ == "__main__":
    p1 = Process(target=process1)
    p1.start()
    p1.join()
