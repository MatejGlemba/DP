import re

import pandas as pd
from ai_tools import violence_checker, text_preprocessing, topic_modeling, image_processing
from typing import Dict, List, Tuple




numOfTopics = 4
decryptedMessages = []
decryptedMessages.append("Hello! I want to kill you I would like to go to dinner tonight. Do you have an idea for a restaurant?")
decryptedMessages.append("Definitely! I want to kill you What cuisines do you prefer?")
decryptedMessages.append("I'd like some good seafood I want to kill you.")
decryptedMessages.append("I have a great tip for a seafood restaurant. I want to kill you. It is a new restaurant with a modern interior and excellent reviews.")
decryptedMessages.append("Sounds good! I want to kill you! What are the typical dishes there?")
decryptedMessages.append("They have a wide selection of seafood including fresh scallops, shrimp, octopus and even crayfish. They also have excellent fish and seafood specialties.")
decryptedMessages.append("Sounds tempting! How about the prices?")
decryptedMessages.append("The prices are moderate and match the quality of the food. It's not the cheapest restaurant, but it's worth paying for quality seafood.")
decryptedMessages.append("Understood. How are the reservations?")
decryptedMessages.append("I recommend booking a table in advance as this restaurant is often full. Can I look at available dates and reserve a table for you?")
decryptedMessages.append("Yes please! That would be great.")
decryptedMessages.append("Great, I booked a table for two at 7:00pm. I confirm your reservation.")
decryptedMessages.append("John Smith lives in New York and works at Google.")


def demo():
    # s = []
    # s.append("Hello! I want to kill you I would like to go to dinner tonight. Do you have an idea for a restaurant?")
    # df = pd.DataFrame(s, columns =['text'])
    # df['text'].map(lambda x: re.sub('[,\.!?]', '', x))
    # print(df['text'])

    messages, ner_labels = text_preprocessing.process(decryptedMessages, True)

    print("Processed messages : ",messages)
    print("\n")
    print("Found ner_labels", ner_labels)    
    print("----------------------------------------------------------------------------------------------------")
    roomName = 'Ideas for restaurants' 
    imageTopic = 'restaurant'
    roomNameWithImageTopic = [roomName + ' ' + imageTopic]    
    processedRoomNameWithImageTopic = text_preprocessing.process(roomNameWithImageTopic, False)[0]
    print("Processed room name with image topic : ",processedRoomNameWithImageTopic)
    print("\n")
    print("----------------------------------------------------------------------------------------------------")
    description = ['Where to eat and what to cook']            
    description = text_preprocessing.process(description, False)[0]
    print("Processed description : ",description)
    print("\n")
    print("----------------------------------------------------------------------------------------------------")  
    topWords = []
    topWords.extend(processedRoomNameWithImageTopic)
    topWords.extend(description)
    print("room name and description: ",topWords)
    print("\n")
    print("----------------------------------------------------------------------------------------------------")  
    messages.append(processedRoomNameWithImageTopic)
    messages.append(description)
    print("messages with room name and description: ",messages)
    print("\n")
    print("----------------------------------------------------------------------------------------------------")  
    numOfWordsPerTopic = 5    
    model_topics = topic_modeling.runModel(documents=messages, num_topics=numOfTopics, num_words=numOfWordsPerTopic, passes=5, iterations=50, minimum_probability=0.05)
    print("Model topics :")
    for model_topic in model_topics:
        print("Model topic : {}".format(model_topic))
    
    print("----------------------------------------------------------------------------------------------------")   
    model_topics : Dict[str, List[Tuple[float, str]]] = topic_modeling.updatePercentage(model_topics, ner_labels, topWords)
    print("Updated Model Topics weights of ner_labels :")
    for model_topic_num, v in model_topics.items():
        print("Model topic : {} - {}".format(model_topic_num, v))


demo()