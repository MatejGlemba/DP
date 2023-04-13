from typing import Dict, List, Tuple
from pymongo.collection import Collection
from influxdb_client import InfluxDBClient, Point
from influxdb_client.client.write_api import ASYNCHRONOUS

messagesCollectionKeys = ['roomID', 'qrcodeID', 'topicNum']
blacklistCollectionKeys = ['userID', 'topicNum']

entityModelRoom = 'entity-model-room'
entityModelUser = 'entity-model-user'

organization="dmn"
bucket = "topics"
token="6jDTh95X6RUeFedSzJ3B_9LVFSG5g_Ra0HwOZfO_OR-y9am02-WWCx-F1LUIXnhCQEXpbWDIcWpM8Vxefo054Q=="
url="http://localhost:8086"

class InfluxDBHandler:

    def getEntityRoomDBHandler(self):
        return EntityRoomDBHandler(measurement=entityModelRoom)
    
    def getEntityUserDBHandler(self):
        return EntityUserDBHandler(measurement=entityModelUser)
    
class EntityRoomDBHandler:
    def __init__(self, measurement: str) -> None:
        self.__measurement = measurement

    def updateTopics(self, roomID: str, qrcodeID: str, topics: Dict[int, List[Tuple[float, str]]]):
        client = InfluxDBClient(url=url, token=token, org=organization) 

        # read - check if exists
        queryAPI = client.query_api()
        query = f'from(bucket:"{bucket}")\
            |> range(start: -inf)\
            |> filter(fn:(r) => r._measurement == "{self.__measurement}")\
            |> filter(fn:(r) => r.roomID == "{roomID}")\
            |> filter(fn:(r) => r.qrcodeID == "{qrcodeID}")'
            
        results : List[Dict] = []
        result = queryAPI.query(query)
        for table in result:
            for record in table.records:
                results.append(record.values)

        # if exists delete
        if results:
            deleteAPI = client.delete_api()
            for result in results:
                start = result['_start']
                stop = result['_stop']

            predicate = f'_measurement=\"{self.__measurement}\" and roomID=\"{roomID}\" and qrcodeID=\"{qrcodeID}\"'
            deleteAPI.delete(start=start, stop=stop, predicate=predicate, bucket=bucket, org=organization)
            results = []

        # insert new one
        for topicNum, topicList in topics.items():
            point = Point(self.__measurement).tag(messagesCollectionKeys[0], roomID).tag(messagesCollectionKeys[1], qrcodeID)
            point.tag(messagesCollectionKeys[2], topicNum)
        
            for weight, word in topicList:
                point.field(word, weight)

            writeAPI = client.write_api(write_options=ASYNCHRONOUS)
            writeAPI.write(bucket=bucket, record=point)
        
        # close
        client.close()

class EntityUserDBHandler:
    def __init__(self, measurement: str) -> None:
        self.__measurement = measurement

    # there is always only one topic 
    def updateTopics(self, userID: str, topics: Dict[int, List[Tuple[float, str]]]):
        client = InfluxDBClient(url=url, token=token, org=organization) 
        # read - check if exists
        queryAPI = client.query_api()
        query = f'from(bucket:"{bucket}")\
            |> range(start: -inf)\
            |> filter(fn:(r) => r._measurement == "{self.__measurement}")\
            |> filter(fn:(r) => r.userID == "{userID}")'
            
        results : List[Dict] = []
        hate = 0
        spam = 0
        result = queryAPI.query(query)
        for table in result:
            for record in table.records:
                if record['_field'] == 'hateSpeech':
                    hate = int(record['_value'])
                if record['_field'] == 'spamming':
                    spam = int(record['_value'])
                results.append(record.values)

        # if exists delete
        if results:
            deleteAPI = client.delete_api()
            for result in results:
                start = result['_start']
                stop = result['_stop']

            predicate = f'_measurement=\"{self.__measurement}\" and userID=\"{userID}\"'
            deleteAPI.delete(start=start, stop=stop, predicate=predicate, bucket=bucket, org=organization)

        # insert new topics with original flags
        writeAPI = client.write_api(write_options=ASYNCHRONOUS)
        for topicNum, topicList in topics.items():
            point = Point(self.__measurement).tag(blacklistCollectionKeys[0], userID)
            #point.tag(messagesCollectionKeys[1], topicNum)
        
            for weight, word in topicList:
                point.field(word, weight)

            point.field("hateSpeech", hate)
            point.field("spamming", spam)
            writeAPI.write(bucket=bucket, record=point)    

        # close
        client.close()

    def updateHateSpeech(self, userID: str):
        client = InfluxDBClient(url=url, token=token, org=organization) 
        # read - check if exists
        queryAPI = client.query_api()
        query = f'from(bucket:"{bucket}")\
            |> range(start: -inf)\
            |> filter(fn:(r) => r._measurement == "{self.__measurement}")\
            |> filter(fn:(r) => r.userID == "{userID}")'
            
        results : List[Dict] = []
        hateWasPresent = False
        result = queryAPI.query(query)
        for table in result:
            for record in table.records:
                if record['_field'] == 'hateSpeech':
                    hateWasPresent = True
                results.append(record.values)

        # if exists delete
        if results:
            deleteAPI = client.delete_api()
            for result in results:
                start = result['_start']
                stop = result['_stop']

            predicate = f'_measurement=\"{self.__measurement}\" and userID=\"{userID}\"'
            deleteAPI.delete(start=start, stop=stop, predicate=predicate, bucket=bucket, org=organization)
            results = []

        # insert new hate flag with original topics and spamming flag
        writeAPI = client.write_api(write_options=ASYNCHRONOUS)
        point = Point(self.__measurement).tag(blacklistCollectionKeys[0], userID)
    
        for result in results:
            if result['_field'] == 'hateSpeech':
                numOfHate = int(result['_value'])
                numOfHate += 1
                point.field(result['_field'], numOfHate)
            else:
                point.field(result['_field'], result['_value'])

        if not hateWasPresent:
            point.field("hateSpeech", 1)

        writeAPI.write(bucket=bucket, record=point)    

        # close
        client.close()
 
    def updateSpamming(self, userID: str):
        client = InfluxDBClient(url=url, token=token, org=organization) 
        # read - check if exists
        queryAPI = client.query_api()
        query = f'from(bucket:"{bucket}")\
            |> range(start: -inf)\
            |> filter(fn:(r) => r._measurement == "{self.__measurement}")\
            |> filter(fn:(r) => r.userID == "{userID}")'
            
        results : List[Dict] = []
        spamWasPresent = False
        result = queryAPI.query(query)
        for table in result:
            for record in table.records:
                if record['_field'] == 'spamming':
                    spamWasPresent = True
                results.append(record.values)

        # if exists delete
        if results:
            deleteAPI = client.delete_api()
            for result in results:
                start = result['_start']
                stop = result['_stop']

            predicate = f'_measurement=\"{self.__measurement}\" and userID=\"{userID}\"'
            deleteAPI.delete(start=start, stop=stop, predicate=predicate, bucket=bucket, org=organization)
            results = []

        # insert new spam flag with original topics and hateSpeech flag
        writeAPI = client.write_api(write_options=ASYNCHRONOUS)
        point = Point(self.__measurement).tag(blacklistCollectionKeys[0], userID)
    
        for result in results:
            if result['_field'] == 'spamming':
                numOfSpam = int(result['_value'])
                numOfSpam += 1
                point.field(result['_field'], numOfSpam)
            else:
                point.field(result['_field'], result['_value'])

        if not spamWasPresent:
            point.field("spamming", 1)
        
        writeAPI.write(bucket=bucket, record=point)    

        # close
        client.close()
