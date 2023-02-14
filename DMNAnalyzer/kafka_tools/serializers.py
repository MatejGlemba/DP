import json
from json import JSONEncoder
import dataclasses

class KafkaObject:
    def dump(self):
        return self.__dict__

class MessageOutputs(KafkaObject):
    roomID: str    
    qrcodeID: str
    userID: str
    type: str

    def __init__(self, roomID, qrcodeID, userID, type):
        self.roomID = roomID
        self.qrcodeID = qrcodeID
        self.userID = userID
        self.type = type

class ImageData(KafkaObject):
    qrcodeID: str
    image: bytearray

    def __init__(self, qrcodeID, image):
        self.qrcodeID = qrcodeID
        self.image = image

class BlacklistData(KafkaObject):
    roomID: str
    qrcodeID: str
    userID: str
    notes: str

    def __init__(self, roomID, qrcodeID, userID, notes):
        self.roomID = roomID
        self.qrcodeID = qrcodeID
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
    return json.dumps(obj.dump(), cls=KafkaSerializer)