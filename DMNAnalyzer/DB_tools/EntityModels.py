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
    spamming: bool
    blacklistTopics: List

    def __init__(self, userID, hateSpeech, spamming, blacklistTopics: List):
        self.userID = userID
        self.hateSpeech = hateSpeech
        self.spamming = spamming
        self.blacklistTopics = blacklistTopics

