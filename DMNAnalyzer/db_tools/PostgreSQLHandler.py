import multiprocessing
from typing import Dict, List, Tuple
import psycopg2

# Define a global lock for coordination
table_creation_lock = multiprocessing.Lock()

class PostgresDBHandler:
    def __init__(self, postgresDB : str, postgresUser : str, postgresPass : str, postgresHost : str, postgresPort : str) -> None:
        self.__conn = psycopg2.connect(dbname=postgresDB,user=postgresUser,password=postgresPass,host=postgresHost,port=postgresPort)
        self.__createTables()

    def __createTables(self):
        cursor = self.__conn.cursor()
        create_table_room_query = """
            CREATE TABLE IF NOT EXISTS entity_model_room (
                id SERIAL PRIMARY KEY,
                qrcode_id VARCHAR(100),
                room_id VARCHAR(100),
                topic_num INTEGER,
                word VARCHAR(100),
                weight REAL
            );
        """
        create_table_room_flags_query = """
            CREATE TABLE IF NOT EXISTS entity_model_room_flags (
                id SERIAL PRIMARY KEY,
                room_id VARCHAR(100) UNIQUE,
                sexual INTEGER,
                hate INTEGER,
                violence INTEGER,
                self_harm INTEGER,
                sexual_minors INTEGER,
                hate_threatening INTEGER,
                violence_graphic INTEGER
            );
        """
        create_table_user_topics_query = """
            CREATE TABLE IF NOT EXISTS entity_model_user_topics (
                id SERIAL PRIMARY KEY,
                user_id VARCHAR(100),
                word VARCHAR(100),
                weight REAL
            );
        """
        create_table_user_flags_query = """
            CREATE TABLE IF NOT EXISTS entity_model_user_flags (
                id SERIAL PRIMARY KEY,
                user_id VARCHAR(100) UNIQUE,
                hate_speech INTEGER
            );
        """
        with table_creation_lock:
            try:
                cursor.execute(create_table_room_query)
                cursor.execute(create_table_room_flags_query)
                cursor.execute(create_table_user_topics_query)
                cursor.execute(create_table_user_flags_query)
                self.__conn.commit()        
            except Exception as e:
                print(f"Error creating table: {e}")
            finally:
                cursor.close() 

    def getEntityRoomDBHandler(self):
        return EntityRoomDBHandler(conn=self.__conn)
    
    def getEntityUserDBHandler(self):
        return EntityUserDBHandler(conn=self.__conn)
    
    def close(self):
        self.__conn.close()
    
class EntityRoomDBHandler:
    def __init__(self, conn) -> None:
        self.__conn = conn

    def updateTopics(self, roomID: str, qrcodeID: str, topics: Dict[int, List[Tuple[float, str]]], overallTopics: Dict[int, List[Tuple[float, str]]]):
        cursor = self.__conn.cursor()
        
        # Check if the row already exists in the table
        cursor.execute("""
            SELECT * FROM entity_model_room 
            WHERE room_id = %s AND qrcode_id = %s
        """, (roomID, qrcodeID))

        row_count = cursor.fetchone()
        
        if row_count is not None:
            cursor.execute("""
                DELETE FROM entity_model_room WHERE qrcode_id = %s AND room_id = %s
            """, (qrcodeID, roomID))
            
        values = []
        for _, topicList in overallTopics.items():
            for weight, word in topicList:
                values.append((qrcodeID, roomID, 0, word, weight))

        for topicNum, topicList in topics.items():
            for weight, word in topicList:
                values.append((qrcodeID, roomID, int(topicNum)+1, word, weight))

        cursor.executemany("""
            INSERT INTO entity_model_room (qrcode_id, room_id, topic_num, word, weight) 
            VALUES (%s, %s, %s, %s, %s)
        """, values)

        self.__conn.commit()
        cursor.close()
        
    def updateViolence(self, roomID: str, flags: Dict):
        cursor = self.__conn.cursor()
        
        # Check if the row already exists in the table
        cursor.execute("""
            SELECT * FROM entity_model_room_flags 
            WHERE room_id = %s
        """, (roomID,))

        row_count = cursor.fetchone()
        
        # prepare flags
        insertValues = []
        updateValues = []
        flagValues = []
        insertValues.append(roomID)
        for _, value in flags.items():
            valueB = bool(value)
            if valueB:
                flagValues.append(1)
            else:
                flagValues.append(0)    
        insertValues.extend(flagValues)
        updateValues.extend(flagValues)
        updateValues.append(roomID)

        # update
        if row_count is not None:
            cursor.execute("""
                UPDATE entity_model_room_flags 
                SET 
                    sexual = sexual + %s, 
                    hate = hate + %s, 
                    violence = violence + %s, 
                    self_harm = self_harm + %s, 
                    sexual_minors = sexual_minors + %s, 
                    hate_threatening = hate_threatening + %s, 
                    violence_graphic = violence_graphic + %s 
                WHERE 
                    room_id = %s;
            """, updateValues)
        # insert
        else:
            cursor.execute("""
                INSERT INTO entity_model_room_flags (room_id, sexual, hate, violence, self_harm, sexual_minors, hate_threatening, violence_graphic) 
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s) 
            """, insertValues)

        self.__conn.commit()
        cursor.close()

class EntityUserDBHandler:
    def __init__(self, conn) -> None:
        self.__conn = conn

    # there is always only one topic 
    def updateTopics(self, userID: str, topics: Dict[int, List[Tuple[float, str]]]):
        cursor = self.__conn.cursor()
    
        # Check if the row already exists in the table
        cursor.execute("""
            SELECT * FROM entity_model_user_topics
            WHERE user_id = %s
        """, (userID,))

        row_count = cursor.fetchone()

        if row_count is not None:
            cursor.execute("""
                DELETE FROM entity_model_user_topics WHERE user_id = %s
            """, (userID,))
            
        values = []
        for _, topicList in topics.items():
            for weight, word in topicList:
                values.append((userID, word, weight))

        cursor.executemany("""
            INSERT INTO entity_model_user_topics (user_id, word, weight) 
            VALUES (%s, %s, %s)
        """, values)

        self.__conn.commit()
        cursor.close()

    def updateHateSpeech(self, userID: str):
        cursor = self.__conn.cursor()

        # Perform an upsert (insert or update)
        cursor.execute("""
            INSERT INTO entity_model_user_flags (user_id, hate_speech) 
            VALUES (%s, %s) 
            ON CONFLICT (user_id) DO UPDATE 
            SET hate_speech = entity_model_user_flags.hate_speech + 1
        """, (userID,1))

        self.__conn.commit()
        cursor.close()
 
