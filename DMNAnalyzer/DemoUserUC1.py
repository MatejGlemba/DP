from multiprocessing import Process
from time import sleep
from kafka_tools import KafkaHandler
from kafka_tools.serializers import BlacklistData, MessageData
import csv

def process1():
    #UC1_1
    messageTopicHandler = KafkaHandler.MessageTopicHandler()
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
    blacklistDataTopicHandler = KafkaHandler.RoomDataAndBlacklistTopicHandler()
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
            blacklistDataTopicHandler.produce(BlacklistData(row_dict['userID'], row_dict['notes']))
        blacklistDataTopicHandler.flush()

    #UC1_3
    messageTopicHandler = KafkaHandler.MessageTopicHandler()
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

def dbCleanup():
    from influxdb_client import InfluxDBClient
    import psycopg2
    from pymongo import MongoClient
    # MongoDB cleanup
    client = MongoClient("mongodb://root:rootpassword@localhost:27017/")
    db = client['analyzerDB-inputs']
    collection = db['topic-model-messages']
    collection.drop()
    collection = db['topic-model-room']
    collection.drop()
    collection = db['topic-model-user']
    collection.drop()

    # db = client['analyzerDB-outputs']
    # collection = db['entity-model-room']
    # collection.drop()
    # collection = db['entity-model-user']
    # collection.drop()


    # InfluxDB cleanup
    # client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
    
    # query = 'from(bucket:"topics")\
    # |> range(start: -inf)\
    # |> filter(fn:(r) => r._measurement == "entity-model-room")'
    
    # results = []
    # result = client.query_api().query(query)
    # for table in result:
    #     for record in table.records:
    #         results.append(record.values)

    # delete_api = client.delete_api()
    # if results:
    #     for result in results:
    #         start = result['_start']
    #         stop = result['_stop']
    #         measurement = result['_measurement']

    #         predicate = f'_measurement=\"{measurement}\"'
    #         delete_api.delete(start=start, stop=stop, predicate=predicate, bucket='topics', org='dmn')

    # query = 'from(bucket:"topics")\
    # |> range(start: -inf)\
    # |> filter(fn:(r) => r._measurement == "entity-model-user")'
    
    # results = []
    # result = client.query_api().query(query)
    # for table in result:
    #     for record in table.records:
    #         results.append(record.values)

    # delete_api = client.delete_api()
    # if results:
    #     for result in results:
    #         start = result['_start']
    #         stop = result['_stop']
    #         measurement = result['_measurement']

    #         predicate = f'_measurement=\"{measurement}\"'
    #         delete_api.delete(start=start, stop=stop, predicate=predicate, bucket='topics', org='dmn')

    dbName='analyzerDB'
    user='admin'
    password='password'
    host='localhost'
    port='5434'
    conn = psycopg2.connect(dbname=dbName,user=user,password=password,host=host,port=port)
    cursor = conn.cursor() 
    cursor.execute(
        """
        DROP TABLE entity_model_room
        """
    )
    cursor.execute(
        """
        DROP TABLE entity_model_user_topics
        """
    )
    cursor.execute(
        """
        DROP TABLE entity_model_user_flags
        """
    )
    conn.commit()
    cursor.close()
    conn.close()


if __name__ == "__main__":
   # dbCleanup()
    p1 = Process(target=process1)
    p1.start()
    p1.join()
