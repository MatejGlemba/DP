from dataclasses import dataclass
from typing import Type
from dacite import from_dict
from dacite.core import T

@dataclass
class KafkaDeserializerObject:
  pass

@dataclass
class RoomData(KafkaDeserializerObject):
  qrcodeID: str
  photoPath: str
  description: str
  roomName: str

@dataclass
class UserData(KafkaDeserializerObject):
  userID: str
  notes: str

@dataclass
class MessageData(KafkaDeserializerObject):
  roomID: str
  qrcodeID: str
  userID: str
  data: str


def deserialize(jsonValue: bytes, dataClass: Type[T]) -> T:
  return from_dict(data_class=dataClass, data=eval(jsonValue))