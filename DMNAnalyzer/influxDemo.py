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
    client = InfluxDBClient(url="http://localhost:8086", token="K3O8XkemqLaTIUEUZPc0ZgyMGtesu9GE4UDGUwQKK5oyOCplNAZ8fHBjpUIwFusTfNOoglKeqm43yAbFqLXZ8w==", org="dmn")

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
    client = InfluxDBClient(url="http://localhost:8086", token="K3O8XkemqLaTIUEUZPc0ZgyMGtesu9GE4UDGUwQKK5oyOCplNAZ8fHBjpUIwFusTfNOoglKeqm43yAbFqLXZ8w==", org="dmn")
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
    client = InfluxDBClient(url="http://localhost:8086", token="K3O8XkemqLaTIUEUZPc0ZgyMGtesu9GE4UDGUwQKK5oyOCplNAZ8fHBjpUIwFusTfNOoglKeqm43yAbFqLXZ8w==", org="dmn")
    # Define the data to insert
    room_id = 'room444'
    qrcode_id = 'qrcode476'
    word_weights = {'hello': 0.5, 'world': 0.3, 'foo': 0.2}

    # Define the measurement name
    measurement_name = 'topic-model-room'
    query = f'from(bucket:\"dmnOutputs\") |> range(start: -inf) |> filter(fn: (r) => r.roomID == \"{room_id}\" and r.qrcodeID == \"{qrcode_id}\")'
    s = client.query_api().query(query)
    d = s.to_json()
    print(d)

def deleteData():
    room_id = 'room123'
    qrcode_id = 'qrcode456'
    client = InfluxDBClient(url="http://localhost:8086", token="K3O8XkemqLaTIUEUZPc0ZgyMGtesu9GE4UDGUwQKK5oyOCplNAZ8fHBjpUIwFusTfNOoglKeqm43yAbFqLXZ8w==", org="dmn")
   
    delete_api = client.delete_api()

    """
    Delete Data
    """
    start = "1730-12-19T21:51:54.998196+00:00"
    stop = datetime.now()
    predicate = f'_measurement="topic-model-room"'
    s  = delete_api.delete(start=start, stop=stop, predicate=predicate, bucket='dmnOutputs', org='dmn')
    print(s)
    """
    Close client
    """
    client.close()

#insertData()

deleteData()
#insert()
readData()