from typing import List

class RoomEntity:
    roomID: str
    qrcodeID: str
    topics: List

    def __init__(self, roomID: str, qrcodeID: str, topics):
        self.roomID = roomID
        self.qrcodeID = qrcodeID
        self.topics = topics

class UserEntity:
    userID: str
    hateSpeech: bool
    violence: bool
    blacklistTopics: List

    def __init__(self, userID, hateSpeech, violence, blacklistTopics: List):
        self.userID = userID
        self.hateSpeech = hateSpeech
        self.violence = violence
        self.blacklistTopics = blacklistTopics

