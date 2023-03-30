from typing import List, Tuple
from pymongo import MongoClient
from pymongo.collection import Collection
from kafka_tools.deserializers import MessageData, RoomData, BlacklistData
from DB_tools.EntityModels import RoomEntity, UserEntity
from influxdb_client import InfluxDBClient, Point
from influxdb_client.client.write_api import SYNCHRONOUS, ASYNCHRONOUS

messagesCollectionKeys = ['roomID', 'qrcodeID']
roomCollectionKeys = ['qrcodeID']
blacklistCollectionKeys = ['userID']

topicModelMessages = 'topic-model-messages'
topicModelRoom = 'topic-model-room'
topicModelUser = 'topic-model-user'

entityModelRoom = 'entity-model-room'
entityModelUser = 'entity-model-user'

class DBHandler:
    def __init__(self) -> None:
        self.__dbHandlerInputs = InfluxDBClient(url="http://localhost:8086", token='K3O8XkemqLaTIUEUZPc0ZgyMGtesu9GE4UDGUwQKK5oyOCplNAZ8fHBjpUIwFusTfNOoglKeqm43yAbFqLXZ8w==', org="dmn")
        self.__dbHandlerOutputs = InfluxDBClient(url="http://localhost:8086", token='K3O8XkemqLaTIUEUZPc0ZgyMGtesu9GE4UDGUwQKK5oyOCplNAZ8fHBjpUIwFusTfNOoglKeqm43yAbFqLXZ8w==', org="dmn")
        self.__dbInputs = self.__dbHandlerInputs.write_api(write_options=ASYNCHRONOUS)
        self.__dbOutputs = self.__dbHandlerOutputs.write_api(write_options=ASYNCHRONOUS)

    def getBlackListDBHandler(self):
        return BlacklistDBHandler()
    
    def getRoomDBHandler(self):
        return BlacklistDBHandler()
    
    def getMessagesDBHandler(self):
        return MessagesDBHandler(measurement=topicModelMessages)
    
    def getEntityRoomDBHandler(self):
        return EntityRoomDBHandler()
    
    def getEntityUserDBHandler(self):
        return EntityUserDBHandler()

class BlacklistDBHandler:
    def __init__(self, collection: Collection) -> None:
        self.__collection = collection

    def getCollection(self):
        return self.__collection

    # update data for specific room identified by qrcodeID(App)
    def updateBlacklistData(self, key_value: str, newData: BlacklistData):
        self.__collection.replace_one({blacklistCollectionKeys[0]: key_value}, newData.__dict__)
        
    # first data insert for specific room identified by qrcodeID(App)
    def insertBlacklistData(self, roomData: BlacklistData):
        self.__collection.insert_one(roomData.__dict__)

    # find data for specific room identified by qrcodeID(App) -> just for check
    def readBlacklistData(self, key_value: str):
        return self.__collection.find_one({blacklistCollectionKeys[0]: key_value}) 
    
class RoomDBHandler:
    def __init__(self, collection: Collection) -> None:
        self.__collection = collection

    def getCollection(self):
        return self.__collection

    # update data for specific room identified by qrcodeID(App)
    def updateRoomData(self, key_value: str, newData: RoomData):
        self.__collection.replace_one({roomCollectionKeys[0]: key_value}, newData.__dict__)
        
    # first data insert for specific room identified by qrcodeID(App)
    def insertRoomData(self, roomData: RoomData):
        self.__collection.insert_one(roomData.__dict__)

    # find data for specific room identified by qrcodeID(App) -> just for check
    def readRoomData(self, key_value: str):
        return self.__collection.find_one({roomCollectionKeys[0]: key_value})
    
class MessagesDBHandler:
    def __init__(self, measurement: str) -> None:
        self.__measurement = measurement

    def getCollection(self):
        return self.__collection

    # update messages for specific room identified by roomID(Matrix) + qrcodeID(App) -> messages are represented as a list, where new stuff is just appending
    def updateMessagesData(self, key_values, value):
        self.__collection.find_one_and_update({messagesCollectionKeys[0]: key_values[0], messagesCollectionKeys[1]: key_values[1]}, {'$push' : {'data' : value}})
        
    # first insert of data for specific room identified by roomID(Matrix) + qrcodeID(App) -> messages are represented as a list
    def insertMessageData(self, msgData: MessageData):
        newRecord = {
            "measurement" : msgData.roomID, 
            "qrcodeID" : msgData.qrcodeID, 
            "data" : [msgData.data]
        }
        self.__collection.insert_one(newRecord)

    # find messages for specific room identified by roomID(Matrix) + qrcodeID(App) -> just for check
    def readMessagesDataForRoom(self, key_values):
        return self.__collection.find_one({messagesCollectionKeys[0]: key_values[0], messagesCollectionKeys[1]: key_values[1]})

class EntityRoomDBHandler:
    def __init__(self, collection: Collection) -> None:
        self.__collection = collection

    def checkEntityRoom(self, key_values: List[str]):
        return self.__collection.find_one({messagesCollectionKeys[0]: key_values[0], messagesCollectionKeys[1]: key_values[1]})
    
    def insertEntityRoom(self, data: RoomEntity):
        self.__collection.insert_one(data.__dict__)

    def updateTopics(self, key_values: List[str], topics: List):
        self.__collection.find_one_and_update({messagesCollectionKeys[0]: key_values[0], messagesCollectionKeys[1]: key_values[1]}, {'$set' : {'topics' : topics}})
 
class EntityUserDBHandler:
    def __init__(self, collection: Collection) -> None:
        self.__collection = collection

    def insertEntityUser(self, data: UserEntity):
        self.__collection.insert_one(data.__dict__)

    def updateTopics(self, key_value: str, topics: List):
        self.__collection.find_one_and_update({blacklistCollectionKeys[0]: key_value}, {'$push' : {'topics' : topics}})

    def updateHateSpeech(self, key_value: str, hate: bool):
        self.__collection.find_one_and_update({blacklistCollectionKeys[0]: key_value}, { "$set": { "hateSpeech": hate}})
 
    def updateSpamming(self, key_value: str, spam: bool):
        self.__collection.find_one_and_update({blacklistCollectionKeys[0]: key_value}, { "$set": { "spamming": spam}})
