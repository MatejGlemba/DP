from pymongo import MongoClient
from pymongo.collection import Collection

from kafka_tools.deserializers import MessageData, RoomData, BlacklistData

analyzerDB = 'analyzerDB'

messagesCollectionKeys = ['roomID', 'qrcodeID']
roomCollectionKeys = ['qrcodeID']
blacklistCollectionKeys = ['userID']

topicModelMessages = 'topic-model-messages'
topicModelRoom = 'topic-model-room'
topicModelUser = 'topic-model-user'

class DBHandler:
    def __init__(self) -> None:
        self.__dbHandler = MongoClient("mongodb://root:rootpassword@localhost:27017/")
        self.__db = self.__dbHandler[analyzerDB]
    
    def getBlackListDBHandler(self):
        return BlacklistDBHandler(collection=self.__db[topicModelUser])
    
    def getRoomDBHandler(self):
        return BlacklistDBHandler(collection=self.__db[topicModelRoom])
    
    def getMessagesDBHandler(self):
        return MessagesDBHandler(collection=self.__db[topicModelMessages])

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

# def update_data(collection, roomID, qrcodeID, newData):
#     collection.find_one_and_update({'roomID': roomID, 'qrcodeID': qrcodeID}, {'$push': {'data': newData}})

def remove():
    client = MongoClient("mongodb://root:rootpassword@localhost:27017/")
    db = client[analyzerDB]
    collection = db[topicModelMessages]
    collection.drop()
    collection = db[topicModelRoom]
    collection.drop()
    collection = db[topicModelUser]
    collection.drop()
#def remove():
#     client = MongoClient("mongodb://root:rootpassword@localhost:27017/")
#     db = client['testDB']
#     collection = db['collectionDB']
#     collection.delete_one({'roomID': 1})
#     s = collection.find_one({'roomID' : 1})
#     print("after delete", s)

def db_client():
    client = MongoClient("mongodb://root:rootpassword@localhost:27017/")
    db = client[analyzerDB]
    collection = db[topicModelMessages]
    #docs = []
    #doc = []
    #doc.append("mongodb asdfkh asdkfjh askdfha")
    #docs.append(doc)
    #doc = []
    #doc.append("mongodb asdf asvbcdkfjh asksddfha")
    #docs.append(doc)
    #record = {
    #    "roomID" : 1, 
    #    "qrcodeID" : 3, 
    #    "data" : docs
    #}
    #rec = collection.insert_one(record)
    print(collection)
    ss = list(collection.find({}))
    for s in ss:
        print("update", s)
    #newData = ["sadf asdlfkjg sdg"]
    #update_data(collection, 1, 3, newData)
    #s = collection.find_one({'roomID' : 1})
    #print("after update", s )
db_client()
remove()