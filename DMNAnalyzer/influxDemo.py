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
from influxdb_client import InfluxDBClient, Point, WriteOptions, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

def insertData():
    # Set up the InfluxDB client
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")

    # Define the data to insert
    room_id = 'room123'
    qrcode_id = 'qrcode456'
    word_weights = {'hello': 0.5, 'world': 0.3, 'foo': 0.2}
    # Define the measurement name
    measurement_name = 'topic-model-messages'


    # Create a data point

    point = Point(measurement_name).tag('roomID', room_id).tag('qrcodeID', qrcode_id)
    for word, weight in word_weights.items():
        point.field(word, weight)


    # Write the data point to InfluxDB
    write_api = client.write_api(write_options=SYNCHRONOUS)
    write_api.write(bucket="dmn", record=point)

    #----------------------------------------------------------------------
    # Define the data to insert
    room_id = 'room333'
    qrcode_id = 'qrcode466'
    word_weights = {"hello": 0.7, 'world': 0.6, 'foo': 0.2}

    # Define the measurement name
    measurement_name = 'topic-model-messages'

    # Create a data point

    point = Point(measurement_name).tag('roomID', room_id).tag('qrcodeID', qrcode_id)
    for word, weight in word_weights.items():
        point.field(word, weight)

    # Write the data point to InfluxDB
    write_api = client.write_api(write_options=SYNCHRONOUS)
    write_api.write(bucket="dmn", record=point)

    #----------------------------------------------------------------------
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
    point.time(datetime(2022, 1, 1), WritePrecision.NS)
    # Write the data point to InfluxDB
    write_api = client.write_api(write_options=SYNCHRONOUS)
    write_api.write(bucket="dmnOutputs", record=point)


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
 
def bucketApi():
    client = InfluxDBClient(url="http://localhost:8086", token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q==", org="dmn")
    b = client.buckets_api()
    b.delete_bucket(bucket="topic-model-room")
    s = b.find_buckets()
    print(s)
#insertData()


#insert()
#readData()
#deleteData()
#readData()
#insert()
readData()
#bucketApi()