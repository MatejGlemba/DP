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
    db = client[analyzerDBInputs]
    collection = db[topicModelMessages]
    collection.drop()
    collection = db[topicModelRoom]
    collection.drop()
    collection = db[topicModelUser]
    collection.drop()

    db = client[analyzerDBOutputs]
    collection = db[entityModelRoom]
    collection.drop()
    collection = db[entityModelUser]
    collection.drop()

def db_client():
    client = MongoClient("mongodb://root:rootpassword@localhost:27017/")
    db = client[analyzerDBInputs]
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

    #print(collection)
    #ss = list(collection.find({}))
    #for s in ss:
    #    print("update", s)
    

    qrcodeID = 'axAK2s300AFUXR8zfaab'
    ss = list(collection.find({'qrcodeID' : qrcodeID}))
    for s in ss:
        print(type(s))
        print("data", s['roomID'])
    
    #newData = ["sadf asdlfkjg sdg"]
    #update_data(collection, 1, 3, newData)
    #s = collection.find_one({'roomID' : 1})
    #print("after update", s )

#db_client()
#remove()
