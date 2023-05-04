import csv
import os
from ai_tools import text_preprocessing, topic_modeling
from tqdm import tqdm
import gensim.corpora as corpora
import gensim
from nltk.tokenize import word_tokenize
import nltk
from nltk.corpus import stopwords
nltk.download('punkt')
nltk.download('stopwords')

# Read in text and topic files
#text_file = "EMNLP_dataset/dialogues_text.txt"
#topic_file = "EMNLP_dataset/dialogues_topic.txt"

# def dump():
#     with open(text_file, 'r') as file:
#         lines = [line.strip().replace("__eou__", "") for line in file]
#     with open(topic_file, 'r') as file:
#         topics = [int(line.strip()) for line in file]

#     # Create dictionary to store lines by topic
#     lines_by_topic = {}
#     for i, topic in enumerate(topics):
#         # If topic not yet in dictionary, add it
#         if topic not in lines_by_topic:
#             lines_by_topic[topic] = []
#         # Add line to corresponding topic list
#         lines_by_topic[topic].append(lines[i] + "\n")

#     # Write lines to separate files by topic
#     for topic, lines in lines_by_topic.items():
#         filename = f"resources/topic_{topic}.txt"
#         with open(filename, 'w') as file:
#             file.writelines(lines)


def process_DMN():
    roomDescImage = {}
    #{1: Ordinary Life, 2: School Life, 3: Culture & Education,
    #4: Attitude & Emotion, 5: Relationship, 6: Tourism , 
    #7: Health, 8: Work, 9: Politics, 10: Finance}
    roomDescImage[1] = {'room' : 'Just for chatting', 'desc' : 'Ask me anything', 'image' : ['person']}
    roomDescImage[2] = {'room' : 'School stuff', 'desc' : 'Room for schoolmates, homeworks and stuff', 'image' : ['book', 'handbag', 'person']}
    roomDescImage[3] = {'room' : 'Learning languages', 'desc' : 'Common room for language learners', 'image' : ['book']}
    roomDescImage[4] = {'room' : 'Opinions', 'desc' : 'Arguing about anything', 'image' : ['']}
    roomDescImage[5] = {'room' : 'Dating & Relationships', 'desc' : 'Meeting new people and friends', 'image' : ['person']}
    roomDescImage[6] = {'room' : 'Tips for tourism and sightseeing', 'desc' : 'Tips for making different kind of trips', 'image' : ['Person' , 'bicycle']}
    roomDescImage[7] = {'room' : 'General practitioner', 'desc' : 'Room for chatting about my health state', 'image' : ['person', 'cell phone']}
    roomDescImage[8] = {'room' : 'Work duties', 'desc' : 'Discussing about work duties with my colleagues', 'image' : ['laptop']}
    roomDescImage[9] = {'room' : 'Politics', 'desc' : 'Discussing and arguing about politics and actual state', 'image' : ['']}
    roomDescImage[10] = {'room' : 'Investment & Finance', 'desc' : 'Talking about money flow, investment, savings', 'image' : ['money']}

    my_list = range(1,11)
    for item in tqdm(my_list, desc="Processing items", unit="item"):     
        topicFile = 'resources/text_' + str(item) + '.txt'
        dialogs = []
        with open(topicFile, 'r') as file:
            for line in file:
                dialogs.append(line.strip())

        dialogs = [sentence for sublist in dialogs for sentence in sublist.split('.')]
        #print(len(dialogs))
        messages, ner_labels = text_preprocessing.process(dialogs, True)

        # print("Processed messages : ",messages)
        # print("\n")
        # print("Found ner_labels", ner_labels)    
        # print("----------------------------------------------------------------------------------------------------")
        roomName = roomDescImage[item]['room']
        imageTopic = roomDescImage[item]['image']
        temp = []
        for _ in range(3):
            temp.extend(imageTopic)
        roomNameWithImageTopic = [roomName]
        roomNameWithImageTopic.extend(temp)    
        roomNameWithImageTopic = [' '.join(roomNameWithImageTopic)]
        #print(roomNameWithImageTopic)
        processedRoomNameWithImageTopic = text_preprocessing.process(roomNameWithImageTopic, False)[0]
        #print(processedRoomNameWithImageTopic)
        # print("Processed room name with image topic : ",processedRoomNameWithImageTopic)
        # print("\n")
        # print("----------------------------------------------------------------------------------------------------")
        description = roomDescImage[item]['desc']   
        temp = ''
        for _ in range(2):
            temp += ' ' + description
        temp = [temp]
        description = text_preprocessing.process(description, False)[0]
        # print("Processed description : ",description)
        # print("\n")
        # print("----------------------------------------------------------------------------------------------------")  
        topWords = []
        topWords.extend(processedRoomNameWithImageTopic)
        topWords.extend(description)
        topWords = set(topWords)
        # print("room name and description: ",topWords)
        # print("\n")
        # print("----------------------------------------------------------------------------------------------------")  
        messages.append(processedRoomNameWithImageTopic)
        messages.append(description)
        # print("messages with room name and description: ",messages)
        # print("\n")
        # print("----------------------------------------------------------------------------------------------------")  
       # numOfWordsPerTopic = 5    
        numOfTopics = 5
        model_topics = topic_modeling.runModel(documents=messages, num_topics=numOfTopics, passes=10, iterations=50, minimum_probability=0.05)
        # print("Model topics :")
        # for model_topic in model_topics:
        #     print("Model topic : {}".format(model_topic))
        
       
        model_topics = topic_modeling.updatePercentage(model_topics, ner_labels, topWords)
        output_filename = 'resources/topicResultDMN-Nouns_' + str(item) + '.txt'
        with open(output_filename, "w") as file:
            for model_topic_num, v in model_topics.items():
                file.write("Model topic : {} - {} \n".format(model_topic_num, v))
       


# Tokenize input text and remove stopwords
def tokenize_and_remove_stopwords(text):
    stop_words = set(stopwords.words('english'))
    tokens = word_tokenize(text.lower())
    return [token for token in tokens if token not in stop_words and len(token) > 3]

def process():
#    messageHandler = KafkaHandler.MessageTopicHandler('localhost:9094')
    my_list = range(1,11)
    
# Iterate over the list with a progress bar
    for i in tqdm(my_list, desc="Processing items", unit="item"):      
        topicFile = 'resources/text_' + str(i) + '.txt'
        dialogs = []
        with open(topicFile, 'r') as file:
            #contents = file.read()
            for line in file:
                # Process each line here
                dialogs.append(line.strip())
        # Tokenize the documents and remove stopwords
        tokenized_corpus = [tokenize_and_remove_stopwords(doc) for doc in dialogs]

        # Create a dictionary from the tokenized corpus
        dictionary = corpora.Dictionary(tokenized_corpus)

        # Create a bag-of-words representation of the corpus
        bow_corpus = [dictionary.doc2bow(doc) for doc in tokenized_corpus]
        num_topics = 5
        lda = gensim.models.LdaModel(bow_corpus, num_topics=num_topics, id2word=dictionary, passes=10)
        #models.append(lda)

        output_filename = 'resources/topicResultNew_' + str(i) + '.txt'
        with open(output_filename, "w") as file:
            for topic in lda.print_topics():
                file.write(str(topic) + "\n")

if __name__ == "__main__":
    #process()
    process_DMN()