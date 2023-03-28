def runAnalysisOnMessages():
    messageTopicHandler = KafkaHandler.MessageTopicHandler()
    messageOutputsHanlder = KafkaHandler.MessageOutputsTopicHandler()


    incomingMessages = 0
    while True:
        # receive data 
        msgData : MessageData = messageTopicHandler.consume()

        # decrypt data 
        msg : str = crypto.decrypt(msgData.data)
        # check hatespeech
        hate = hatespeechChecker.checkHate(msg)
        if hate:
            messageOutputsHanlder.produce(object=MessageOutputs(msgData.roomID, msgData.qrcodeID, msgData.userID, "hate"))
            MongoDBHandler.updateData("entity-model-user", "userId", msgData.userID, "hate-speech", hate)
        # check spam
        spam = spamChecker.checkSpam(msg)
        if spam:
            messageOutputsHanlder.produce(object=MessageOutputs(msgData.roomID, msgData.qrcodeID, msgData.userID, "spam"))
            MongoDBHandler.updateData("entity-model-user", "userId", msgData.userID, "spamming", hate)
        
        # save to DB
        msg : str = encrypt.encrypt(msg)
        MongoDBHandler.updateData("topic-model-messages", ["roomID", "qrcodeID"], [msgData.roomID, msgData.qrcodeID], "messages", msg)
        
        incomingMessages += 1
        # check limit of new messages -> process text modeling
        if incomingMessages > 15:
            incomingMessages = 0
            messages = MongoDBHandler.readData("topic-model-messages", ["roomID", "qrcodeID"], [msgData.roomID, msgData.qrcodeID])
            messages = crypto.decrypt(messages)
            messages, ner_labels = text_Preprocessing.process(messages)
            model_topics = topic_modeling.runModel(messages)
            model_topics = topic_modeling.updatePercentage(model_topics, ner_labels)
            model_topics = encrypt.encrypt(model_topics)
            ner_labels = encrypt.encrypt(ner_labels)
            MongoDBHandler.updateData("topic-model-outputs", ["roomID", "qrcodeID"], [msgData.roomID, msgData.qrcodeID], ["vectors", "ner_labels"], [model_topics, ner_labels])

def runAnalysisOnBlacklist():
    blacklistTopicHandler = KafkaHandler.BlacklistTopicHandler()

    while True:
        # receive data 
        blkData : BlacklistData = blacklistTopicHandler.consume()
        # decrypt data 
        notes : str = crypto.decrypt(blkData.notes)
        notes, ner_labels = text_Preprocessing.process(notes)
        model_topics = topic_modeling.runModel(notes)
        model_topics = topic_modeling.updatePercentage(model_topics, ner_labels)
        model_topics = encrypt.encrypt(model_topics)
        MongoDBHandler.updateData("entity-model-user", "userId", blkData.userID, "topics", model_topics)

def runAnalysisOnRoom():
    roomDataTopicHandler = KafkaHandler.RoomDataTopicHandler()

    while True:
        # receive data 
        roomData : RoomData = roomDataTopicHandler.consume()
        # decrypt data 
        image : bytearray = crypto.decrypt(roomData.image)
        image_topics = topic_modeling.runImageModel(image)
        image_topics = topic_modeling.updatePercentage(roomData.qrcodeID, image_topics)
        # TODO : resolve calculation of percetange in model with image topics

def serviceStarter():
    p1 = Process(target=runAnalysisOnMessages)
    p1.start()
    p2 = Process(target=runAnalysisOnBlacklist)
    p2.start()
    p3 = Process(target=runAnalysisOnRoom)
    p3.start()

    p1.join()
    p2.join()
    p3.join()