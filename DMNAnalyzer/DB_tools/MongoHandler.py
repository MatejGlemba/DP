from typing import List, Tuple
from pymongo import MongoClient
from pymongo.collection import Collection
from kafka_tools.deserializers import MessageData, RoomData, BlacklistData
from DB_tools.EntityModels import RoomEntity, UserEntity


analyzerDBInputs = 'analyzerDB-inputs'
analyzerDBOutputs = 'analyzerDB-outputs'

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
        self.__dbHandler = MongoClient("mongodb://root:rootpassword@localhost:27017/")
        self.__dbInputs = self.__dbHandler[analyzerDBInputs]
        self.__dbOutputs = self.__dbHandler[analyzerDBOutputs]

    def getBlackListDBHandler(self):
        return BlacklistDBHandler(collection=self.__dbInputs[topicModelUser])
    
    def getRoomDBHandler(self):
        return BlacklistDBHandler(collection=self.__dbInputs[topicModelRoom])
    
    def getMessagesDBHandler(self):
        return MessagesDBHandler(collection=self.__dbInputs[topicModelMessages])
    
    def getEntityRoomDBHandler(self):
        return EntityRoomDBHandler(collection=self.__dbOutputs[entityModelRoom])
    
    def getEntityUserDBHandler(self):
        return EntityUserDBHandler(collection=self.__dbOutputs[entityModelUser])

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
    def __init__(self, collection: Collection) -> None:
        self.__collection = collection

    def getCollection(self):
        return self.__collection

    # update messages for specific room identified by roomID(Matrix) + qrcodeID(App) -> messages are represented as a list, where new stuff is just appending
    def updateMessagesData(self, key_values, value):
        self.__collection.find_one_and_update({messagesCollectionKeys[0]: key_values[0], messagesCollectionKeys[1]: key_values[1]}, {'$push' : {'data' : value}})
        
    # first insert of data for specific room identified by roomID(Matrix) + qrcodeID(App) -> messages are represented as a list
    def insertMessageData(self, msgData: MessageData):
        newRecord = {
            "roomID" : msgData.roomID, 
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