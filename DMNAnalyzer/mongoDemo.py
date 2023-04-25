from pymongo import MongoClient


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


def remove():
    client = MongoClient("mongodb://root:rootpassword@localhost:27017/")
    db = client['analyzerDB-inputs']
    collection = db['topic-model-messages']
    collection.drop()
    collection = db['topic-model-room']
    collection.drop()
    collection = db['topic-model-user']
    collection.drop()

def db_client():
    client = MongoClient("mongodb://root:rootpassword@localhost:27017/")
    db = client[analyzerDBInputs]
    collection = db[topicModelRoom]

    print(collection)
    ss = list(collection.find({}))
    for s in ss:
        print("update", s)
    
    collection = db[topicModelMessages]

    print(collection)
    ss = list(collection.find({}))
    for s in ss:
        print("update", s)

    collection = db[topicModelUser]

    print(collection)
    ss = list(collection.find({}))
    for s in ss:
        print("update", s)

db_client()
#remove()
