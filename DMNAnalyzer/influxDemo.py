# from influxdb import InfluxDBClient

# # Connect to InfluxDB
# client = InfluxDBClient(host='localhost', port=8086, database='mydb')

# # Define the data to insert
# room_id = 'room123'
# qrcode_id = 'qrcode456'
# word_weights = {'hello': 0.5, 'world': 0.3, 'foo': 0.2}

# # Define the measurement name
# measurement_name = 'topics'

# # Create a dictionary for the tags
# tags = {'roomID': room_id, 'qrcodeID': qrcode_id}

# # Create a list of dictionaries for the fields
# fields = [{'word': word, 'weight': weight} for word, weight in word_weights.items()]

# # Create a dictionary for the data point
# data_point = {
#     'measurement': measurement_name,
#     'tags': tags,
#     'fields': fields,
# }

# # Write the data point to InfluxDB
# client.write_points([data_point])

from datetime import datetime
from typing import Dict, List
from influxdb_client import InfluxDBClient, Point, WriteOptions, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

def insertDataMultipleMatrixRoomsForQrCodeRoomWithUpdate():
    # Set up the InfluxDB client
    organization = 'dmn'
    bucket = 'topics'
    measurement = 'temp'
    qrcodeID = 'qrcode1'
    roomID = 'room1'
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org=organization)
   

    topics = {'0' : [(0.6, 'ha'), (0.2, 'halo'), (0.2, 'ha')],  '1' : [(0.2, 'alo'), (0.3, 'hao'), (0.4, 'haloh')]}


    # read - check if exists
    queryAPI = client.query_api()
    query = f'from(bucket:"{bucket}")\
        |> range(start: -inf)\
        |> filter(fn:(r) => r._measurement == "{measurement}")\
        |> filter(fn:(r) => r.qrcodeID == "{qrcodeID}")\
        |> group(columns: ["roomID"])'
        
    results : List[Dict] = []
    result = queryAPI.query(query)
    for table in result:
        for record in table.records:
            print(record)
            results.append(record.values)
    

    # # if exists delete
    # if results:
    #     deleteAPI = client.delete_api()
    #     for result in results:
    #         start = result['_start']
    #         stop = result['_stop']

    #     predicate = f'_measurement=\"{measurement}\" and qrcodeID=\"{qrcodeID}\"'
    #     deleteAPI.delete(start=start, stop=stop, predicate=predicate, bucket=bucket, org=organization)
    #     # results = []

    # insert new one
    # for topicNum, topicList in topics.items():
    #     point = Point(measurement).tag('roomID', roomID).tag('qrcodeID', qrcodeID)
    #     point.tag('topicNum', topicNum)
    
    #     for weight, word in topicList:
    #         point.field(word, weight)

    #     writeAPI = client.write_api(write_options=SYNCHRONOUS)
    #     writeAPI.write(bucket=bucket, record=point)
    
    # roomID = 'room2'
    #     # insert new one
    # for topicNum, topicList in topics.items():
    #     point = Point(measurement).tag('roomID', roomID).tag('qrcodeID', qrcodeID)
    #     point.tag('topicNum', topicNum)
    
    #     for weight, word in topicList:
    #         point.field(word, weight)

    #     writeAPI = client.write_api(write_options=SYNCHRONOUS)
    #     writeAPI.write(bucket=bucket, record=point)
    # roomID = 'room3'
    #     # insert new one
    # for topicNum, topicList in topics.items():
    #     point = Point(measurement).tag('roomID', roomID).tag('qrcodeID', qrcodeID)
    #     point.tag('topicNum', topicNum)
    
    #     for weight, word in topicList:
    #         point.field(word, weight)

    #     writeAPI = client.write_api(write_options=SYNCHRONOUS)
    #     writeAPI.write(bucket=bucket, record=point)
    # close
    client.close()


insertDataMultipleMatrixRoomsForQrCodeRoomWithUpdate()

def insert():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
     # Define the data to insert
    room_id = 'room444'
    qrcode_id = 'qrcode476'
    word_weights = {'halohalo': 0.2}

    # Define the measurement name
    measurement_name = 'topic-model-room'


    # Create a data point
    point = Point(measurement_name).tag('roomID', room_id).tag('qrcodeID', qrcode_id)
    for word, weight in word_weights.items():
        point.field(word, weight)
    point.time(datetime(2020, 1, 1), WritePrecision.S)
    # Write the data point to InfluxDB
    write_api = client.write_api(write_options=SYNCHRONOUS)
    write_api.write(bucket="dmnOutputs", record=point)
    client.close()
    
def insertUser():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
     # Define the data to insert
    userID = 'abc'
    word_weights = {'halohalo': 0.2}

    # Define the measurement name
    measurement_name = 'topic-model-user'


    # Create a data point
    point = Point(measurement_name).tag('userID', userID)
    for word, weight in word_weights.items():
        point.field(word, weight)
    point.time(datetime(2020, 1, 1), WritePrecision.S)
    # Write the data point to InfluxDB
    write_api = client.write_api(write_options=SYNCHRONOUS)
    write_api.write(bucket="topics", record=point)
    point = Point(measurement_name).tag('userID', userID)
    point.field('hateSpeech', 0)
    write_api.write(bucket="topics", record=point)
    point = Point(measurement_name).tag('userID', userID)
    point.field('spamming', 1)
    write_api.write(bucket="topics", record=point)

    client.close()

def readData():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
    # Define the data to insert
    room_id = 'room444'
    qrcode_id = 'qrcode476'
    word_weights = {'hello': 0.5, 'world': 0.3, 'foo': 0.2}

    # Define the measurement name
    measurement_name = 'topic-model-room'
    #query = f'from(bucket:\"dmnOutputs\") |> range(start: -inf) |> filter(fn: (r) => r.roomID == \"{room_id}\" and r.qrcodeID == \"{qrcode_id}\")'
    
    
    query = 'from(bucket:"topics")\
|> range(start: -inf)\
|> filter(fn:(r) => r._measurement == "topic-model-room")'
    
    results = []
    result = client.query_api().query(query)
    for table in result:
        for record in table.records:
            #print(record.values)
            #print(record.get_start())
            #print(record.get_stop())
            results.append(record.values)

    print(results)

def readUser():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
    # Define the data to insert
    userID = 'abc'

    # Define the measurement name
    measurement_name = 'topic-model-user'
    #query = f'from(bucket:\"dmnOutputs\") |> range(start: -inf) |> filter(fn: (r) => r.roomID == \"{room_id}\" and r.qrcodeID == \"{qrcode_id}\")'
    
    
    query = 'from(bucket:"topics")\
|> range(start: -inf)\
|> filter(fn:(r) => r._measurement == "topic-model-user")\
|> filter(fn:(r) => r.userID == "abc")'
    
    results = []
    result = client.query_api().query(query)
    for table in result:
        for record in table.records:
            #print(record.values)
            #print(record.get_start())
            #print(record.get_stop())
            results.append(record.values)

    print(results)

def deleteData():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
   
    query = 'from(bucket:"dmnOutputs")\
    |> range(start: -inf)\
    |> filter(fn:(r) => r._measurement == "topic-model-room")\
    |> filter(fn:(r) => r.roomID == "room444")\
    |> filter(fn:(r) => r.qrcodeID == "qrcode476")'
    
    results = {}
    result = client.query_api().query(query)
    for table in result:
        for record in table.records:
            results = record.values

    delete_api = client.delete_api()

    """
    Delete Data
    """
    start = results['_start']
    stop = results['_stop']
    measurement = results['_measurement']
    qrcodeID = results['qrcodeID']
    roomID = results['roomID']

    predicate = f'_measurement=\"{measurement}\" and roomID=\"{roomID}\" and qrcodeID=\"{qrcodeID}\"'
    print(predicate)
    print("start", start)
    print("stop", stop)
    s  = delete_api.delete(start=start, stop=stop, predicate=predicate, bucket='dmnOutputs', org='dmn')
    print(s)
    """
    Close client
    """
    client.close()
 
def deleteUser():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
   
    query = 'from(bucket:"topics")\
    |> range(start: -inf)\
    |> filter(fn:(r) => r._measurement == "topic-model-user")\
    |> filter(fn:(r) => r.userID == "abc")'
    
    results = {}
    result = client.query_api().query(query)
    for table in result:
        for record in table.records:
            results = record.values

    delete_api = client.delete_api()

    """
    Delete Data
    """
    start = results['_start']
    stop = results['_stop']
    measurement = results['_measurement']
    userID = results['userID']

    predicate = f'_measurement=\"{measurement}\" and userID=\"{userID}\"'
    print(predicate)
    print("start", start)
    print("stop", stop)
    s  = delete_api.delete(start=start, stop=stop, predicate=predicate, bucket='topics', org='dmn')
    print(s)
    """
    Close client
    """
    client.close()

def bucketApi():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
    b = client.buckets_api()
    b.delete_bucket(bucket="topic-model-room")
    s = b.find_buckets()
    print(s)

def userDemo():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
    bucket = "topics"
    userID = 'abc'
    measurement = 'topic-model-user'
    # read - check if exists
    queryAPI = client.query_api()
    query = f'from(bucket:"{bucket}")\
        |> range(start: -inf)\
        |> filter(fn:(r) => r._measurement == "{measurement}")\
        |> filter(fn:(r) => r.userID == "{userID}")'
        
    results : List[Dict] = []
    result = queryAPI.query(query)
    for table in result:
        for record in table.records:
            results.append(record.values)
    print("records", results)
    # if exists delete
    if results:
        deleteAPI = client.delete_api()
        for result in results:
            if 'hateSpeech' == result['_field']:
                hateSpeech = result['_value']
            if 'spamming' == result['_field']:
                spamming = result['_value']
            start = result['_start']
            stop = result['_stop']
        print("start",start)
        print("stop",stop)
        print("hate", hateSpeech)
        print("spam", spamming)
        predicate = f'_measurement=\"{measurement}\" and userID=\"{userID}\"'
        deleteAPI.delete(start=start, stop=stop, predicate=predicate, bucket=bucket, org='dmn')
        results = []

    # insert new topics with original flags
    topics = {'halohalo': 0.8}
    writeAPI = client.write_api(write_options=SYNCHRONOUS)
    #for topicNum, topicList in topics.items():
    point = Point(measurement).tag('userID', userID)
    #point.tag(messagesCollectionKeys[1], topicNum)

    for word, weight in topics.items():
        point.field(word, weight)
    print(hateSpeech)
    print(type(hateSpeech))
    point.field("hateSpeech", hateSpeech)
    point.field("spamming", spamming)

    print(point.__dict__)
    writeAPI.write(bucket=bucket, record=point)    

    # close
    client.close()

def clearDB():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
    
    query = 'from(bucket:"topics")\
    |> range(start: -inf)\
    |> filter(fn:(r) => r._measurement == "entity-model-room")'
    
    results = {}
    result = client.query_api().query(query)
    for table in result:
        for record in table.records:
            results = record.values

    delete_api = client.delete_api()

    """
    Delete Data
    """
    if results:
        start = results['_start']
        stop = results['_stop']
        measurement = results['_measurement']

        predicate = f'_measurement=\"{measurement}\"'
        # print(predicate)
        # print("start", start)
        # print("stop", stop)
        s  = delete_api.delete(start=start, stop=stop, predicate=predicate, bucket='topics', org='dmn')
        print(s)

    query = 'from(bucket:"topics")\
    |> range(start: -inf)\
    |> filter(fn:(r) => r._measurement == "entity-model-user")'
    
    results = {}
    result = client.query_api().query(query)
    for table in result:
        for record in table.records:
            results = record.values

    delete_api = client.delete_api()

    """
    Delete Data
    """
    if results:
        start = results['_start']
        stop = results['_stop']
        measurement = results['_measurement']

        predicate = f'_measurement=\"{measurement}\"'
        # print(predicate)
        # print("start", start)
        # print("stop", stop)
        s  = delete_api.delete(start=start, stop=stop, predicate=predicate, bucket='topics', org='dmn')
        print(s)


    query = 'from(bucket:"topics")\
    |> range(start: -inf)\
    |> filter(fn:(r) => r._measurement == "temp")'
    
    results = {}
    result = client.query_api().query(query)
    for table in result:
        for record in table.records:
            results = record.values

    delete_api = client.delete_api()

    """
    Delete Data
    """
    if results:
        start = results['_start']
        stop = results['_stop']
        measurement = results['_measurement']

        predicate = f'_measurement=\"{measurement}\"'
        # print(predicate)
        # print("start", start)
        # print("stop", stop)
        s  = delete_api.delete(start=start, stop=stop, predicate=predicate, bucket='topics', org='dmn')
        print(s)

    """
    Close client
    """
    client.close()


# insertData()


#insert()
#readData()
#deleteData()
#readData()
#insert()
#readData()
#bucketApi()

#insertUser()
#readUser()
#userDemo()
#readUser()

#deleteUser()

clearDB()