from typing import List, Tuple
from pymongo import MongoClient
from pymongo.collection import Collection

class DBHandler:
    def __init__(self, uri : str) -> None:
        self.__dbHandler = MongoClient(uri)
        self.__dbInputs = self.__dbHandler['analyzerDB-inputs']

    def getBlackListDBHandler(self):
        return BlacklistDBHandler(collection=self.__dbInputs['topic-model-user'])
    
    def getMessagesDBHandler(self):
        return MessagesDBHandler(collection=self.__dbInputs['topic-model-messages'])
    
    def getRoomDBHandler(self):
        return RoomDBHandler(collection=self.__dbInputs['topic-model-room'])
    
class BlacklistDBHandler:
    def __init__(self, collection: Collection) -> None:
        self.__collection = collection

    def getCollection(self):
        return self.__collection

    # update data for specific user identified by userID
    def updateUserData(self, userID: str, notes: str):
        self.__collection.update_one({'userID': userID}, {'$push' : {'data' : notes}})
        
    # first data insert for specific user identified by userID(App)
    def insertUserData(self, userID: str, notes: str):
        newRecord = {
            "userID" : userID, 
            "data" : [notes]
        }
        self.__collection.insert_one(newRecord)

    # find data for specific user identified by userID(App) -> just for check
    def readUserData(self, userID: str):
        return self.__collection.find_one({'userID': userID}) 
      
class RoomDBHandler:
    def __init__(self, collection: Collection) -> None:
        self.__collection = collection

    def getCollection(self):
        return self.__collection

    # update data for specific room identified by qrcodeID(App)
    def updateRoomNameData(self, qrcodeID: str, roomName: str):
        self.__collection.update_one({'qrcodeID': qrcodeID}, {'$set' : {'roomName' : roomName}})

    # update data for specific room identified by qrcodeID(App)
    def updateRoomDescriptionData(self, qrcodeID: str, description: str):
        self.__collection.update_one({'qrcodeID': qrcodeID}, {'$set' : {'description' : description}})

    # update data for specific room identified by qrcodeID(App)
    def updateRoomImageObjectsData(self, qrcodeID: str, imageObjects: str):
        self.__collection.update_one({'qrcodeID': qrcodeID}, {'$set' : {'imageObjects' : imageObjects}})

    # update data for specific room identified by qrcodeID(App)
    def updateRoomData(self, qrcodeID: str, roomName: str, description: str, imageObjects: str):
        self.__collection.update_one({'qrcodeID': qrcodeID}, {'$set' : {'roomName' : roomName, 'description' : description, 'imageObjects' : imageObjects}})
       
    # first data insert for specific room identified by qrcodeID(App)
    def insertRoomData(self, qrcodeID: str, roomName: str, description: str, imageObjects: str):
        newRecord = {
            "qrcodeID" : qrcodeID, 
            "roomName" : roomName,
            "description" : description,
            "imageObjects" : imageObjects,
        }
        self.__collection.insert_one(newRecord)

    # find data for specific room identified by qrcodeID(App) -> just for check
    def readRoomData(self, qrcodeID: str):
        return self.__collection.find_one({'qrcodeID': qrcodeID}) 

class MessagesDBHandler:
    def __init__(self, collection: Collection) -> None:
        self.__collection = collection

    def getCollection(self):
        return self.__collection

    # update messages for specific room identified by roomID(Matrix) + qrcodeID(App) -> messages are represented as a list, where new stuff is just appending
    def updateMessagesData(self, roomID: str, qrcodeID: str, value):
        self.__collection.update_one({'roomID': roomID, 'qrcodeID': qrcodeID}, {'$push' : {'data' : value}})

    
    # update messages for all room identified by qrcodeID(App) -> messages are represented as a list, where new stuff is just appending
    def updateMessagesDataForAllRooms(self, qrcodeID, value):
        self.__collection.update_many({'qrcodeID': qrcodeID}, {'$push' : {'data' : value}})


    # first insert of data for specific room identified by roomID(Matrix) + qrcodeID(App) -> messages are represented as a list
    def insertMessageData(self, roomID: str, qrcodeID: str, data: str):
        newRecord = {
            "roomID" : roomID, 
            "qrcodeID" : qrcodeID, 
            "data" : [data]
        }
        self.__collection.insert_one(newRecord)

    # find messages for specific room identified by roomID(Matrix) + qrcodeID(App) -> just for check
    def readMessagesDataForRoom(self, roomID: str, qrcodeID: str):
        return self.__collection.find_one({'roomID': roomID, 'qrcodeID': qrcodeID})
    
    # find messages for specific room identified by qrcodeID(App) 
    def readMessagesDataForAllRooms(self, qrcodeID):
        return list(self.__collection.find({'qrcodeID': qrcodeID}))
