import json
from json import JSONEncoder
import dataclasses

class KafkaObject:
    def dump(self):
        return self.__dict__

class RoomData(KafkaObject):
    qrcodeID: str
    photoPath: str
    description: str
    roomName: str

    def __init__(self, qrcodeID, photoPath, description, roomName):
        self.qrcodeID = qrcodeID
        self.photoPath = photoPath
        self.description = description
        self.roomName = roomName

class UserData(KafkaObject):
    userID: str
    notes: str

    def __init__(self, userID, notes):
        self.userID = userID
        self.notes = notes

class MessageData(KafkaObject):
    roomID: str
    qrcodeID: str
    userID: str
    data: str

    def __init__(self, roomID, qrcodeID, userID, data):
        self.roomID = roomID
        self.qrcodeID = qrcodeID
        self.userID = userID
        self.data = data

class KafkaSerializer(JSONEncoder):

    def default(self, o):
        if dataclasses.is_dataclass(o):
            return dataclasses.asdict(o)
        else:
            return super().default(o)

def serialize(obj: KafkaObject):
    return json.dumps(obj.__dict__, cls=KafkaSerializer)