from pymongo import MongoClient
from pymongo.collection import Collection

class MongoDBHandler:
    def __init__(self) -> None:
        self.__dbHandler = MongoClient("mongodb://root:rootpassword@localhost:27017/")

    def getDB(self, dbName: str):
        return self.__dbHandler[dbName]

    def removeData(self, collection: Collection, key:str , value):
        collection.delete_one({key: value})

    def insertData(self, collection: Collection, value):
        collection.insert_one(value)

    def updateData(self, collection: Collection, keys, key_values, key, value):
        collection.find_one_and_update({keys[0]: key_values[0], keys[1]: key_values[1]}, {'$push' : {key : value}})

    def readData(self, collection: Collection, key : str, keyValue):
        return collection.find_one({key : keyValue})

# def update_data(collection, roomID, qrcodeID, newData):
#     collection.find_one_and_update({'roomID': roomID, 'qrcodeID': qrcodeID}, {'$push': {'data': newData}})

# def remove():
#     client = MongoClient("mongodb://root:rootpassword@localhost:27017/")
#     db = client['testDB']
#     collection = db['collectionDB']
#     collection.delete_one({'roomID': 1})
#     s = collection.find_one({'roomID' : 1})
#     print("after delete", s)

# def db_client():
#     client = MongoClient("mongodb://root:rootpassword@localhost:27017/")
#     db = client['testDB']
#     collection = db['collectionDB']
#     docs = []
#     doc = []
#     doc.append("mongodb asdfkh asdkfjh askdfha")
#     docs.append(doc)
#     doc = []
#     doc.append("mongodb asdf asvbcdkfjh asksddfha")
#     docs.append(doc)
#     record = {
#         "roomID" : 1, 
#         "qrcodeID" : 3, 
#         "data" : docs
#     }
#     rec = collection.insert_one(record)
#     s = collection.find_one({'roomID' : 1})
#     print("before update", s)
#     newData = ["sadf asdlfkjg sdg"]
#     update_data(collection, 1, 3, newData)
#     s = collection.find_one({'roomID' : 1})
#     print("after update", s )

# db_client()
# remove()