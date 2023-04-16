import psycopg2
from DB_tools.PostgreSQLHandler import EntityRoomDBHandler, EntityUserDBHandler, PostgresDBHandler

entityModelRoom = 'entity_model_room'
entityModelUserTopics = 'entity_model_user_topics'
entityModelUserFlags = 'entity_model_user_flags'

# DELETE operation
def delete(model):
    dbName='analyzerDB'
    user='admin'
    password='password'
    host='localhost'
    port='5434'
    conn = psycopg2.connect(dbname=dbName,user=user,password=password,host=host,port=port)
    cursor = conn.cursor() 
    cursor.execute(
        f"""
        DELETE FROM {model}
        """
    )
    conn.commit()
    cursor.close()
    conn.close()

# DELETE operation
def drop(model):
    dbName='analyzerDB'
    user='admin'
    password='password'
    host='localhost'
    port='5434'
    conn = psycopg2.connect(dbname=dbName,user=user,password=password,host=host,port=port)
    cursor = conn.cursor() 
    cursor.execute(
        f"""
        DROP TABLE {model}
        """
    )
    conn.commit()
    cursor.close()
    conn.close()

# READ operation
def read(model):
    dbName='analyzerDB'
    user='admin'
    password='password'
    host='localhost'
    port='5434'
    conn = psycopg2.connect(dbname=dbName,user=user,password=password,host=host,port=port)
    cursor = conn.cursor() 
    cursor.execute(
        f"""
        SELECT * FROM {model}
        """
    )
    data = cursor.fetchall()
    print("Data:")
    for d in data:
        print(d)
    cursor.close()
    conn.close()

def demoRoom():
    postgresHandler : PostgresDBHandler = PostgresDBHandler()
    entityRoomDbHandler : EntityRoomDBHandler = postgresHandler.getEntityRoomDBHandler()

    roomID = "room123"
    qrcodeId = "qr1"
    topics = {"0" : [(0.6, "ha"), (0.2, "halo"), (0.2, "ha")]} 
    entityRoomDbHandler.updateTopics(roomID=roomID, qrcodeID=qrcodeId, topics=topics)
    read(entityModelRoom)
    topics = {"0" : [(0.6, "ha"), (0.5, "halo"), (0.2, "hah")],  "1" : [(0.6, "aloha"), (0.3, "hao"), (0.1, "haloh")]} 
    entityRoomDbHandler.updateTopics(roomID=roomID, qrcodeID=qrcodeId, topics=topics)
    read(entityModelRoom)

def demoUser():
    postgresHandler : PostgresDBHandler = PostgresDBHandler()
    entityUserDbHandler : EntityUserDBHandler = postgresHandler.getEntityUserDBHandler()

    userID = "user123"
    entityUserDbHandler.updateHateSpeech(userID=userID)
    read(entityModelUserFlags)
    entityUserDbHandler.updateHateSpeech(userID=userID)
    read(entityModelUserFlags)
    entityUserDbHandler.updateSpamming(userID=userID)
    read(entityModelUserFlags)
    topics = {"0" : [(0.6, "ha"), (0.2, "halo"), (0.2, "ha")]} 
    entityUserDbHandler.updateTopics(userID=userID, topics=topics)
    read(entityModelUserTopics)
    topics = {"0" : [(0.6, "ha"), (0.5, "halo"), (0.2, "hah")]} 
    entityUserDbHandler.updateTopics(userID=userID, topics=topics)
    read(entityModelUserTopics)
    entityUserDbHandler.updateSpamming(userID=userID)
    read(entityModelUserFlags)

# delete(entityModelRoom)
# drop(entityModelRoom)
# demoRoom()

# delete(entityModelUserFlags)
# drop(entityModelUserFlags)
# delete(entityModelUserTopics)
# drop(entityModelUserTopics)
# demoUser()



drop(entityModelRoom)
drop(entityModelUserTopics)
drop(entityModelUserFlags)

