from dataclasses import dataclass
from typing import Type
from dacite import from_dict
from dacite.core import T

@dataclass
class KafkaDeserializerObject:
  pass

@dataclass
class MessageOutputs(KafkaDeserializerObject):
  roomID: str    
  qrcodeID: str
  userID: str
  type: str

@dataclass
class ImageData(KafkaDeserializerObject):
  qrcodeID: str
  image: bytearray

@dataclass
class BlacklistData(KafkaDeserializerObject):
  roomID: str
  qrcodeID: str
  userID: str
  notes: str

@dataclass
class MessageData(KafkaDeserializerObject):
  roomID: str
  qrcodeID: str
  userID: str
  data: str


def deserialize(jsonValue: str, dataClass: Type[T]) -> T:
  return from_dict(data_class=dataClass, data=eval(jsonValue))