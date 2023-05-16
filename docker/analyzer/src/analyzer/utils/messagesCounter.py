
class Counter:
    def __init__(self) -> None:
        self.roomMessagesCounter = {}

    def inc_counter(self, id1, id2):
        # create a tuple with the two identifiers
        id_tuple = (id1, id2)
        
        # if the tuple is not already in the dictionary, set its count to 1
        if id_tuple not in self.roomMessagesCounter:
            self.roomMessagesCounter[id_tuple] = 1
        # otherwise, increment the existing count
        else:
            self.roomMessagesCounter[id_tuple] += 1

    def get_counter(self, id1, id2):
        # create a tuple with the two identifiers
        id_tuple = (id1, id2)
        if id_tuple not in self.roomMessagesCounter:
            pass
        # otherwise, increment the existing count
        else:
            return self.roomMessagesCounter[id_tuple]

    def set_counter(self, id1, id2):
        # create a tuple with the two identifiers
        id_tuple = (id1, id2)
        if id_tuple not in self.roomMessagesCounter:
            pass
        # otherwise, increment the existing count
        else:
            self.roomMessagesCounter[id_tuple] = 0