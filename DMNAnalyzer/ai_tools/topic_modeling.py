import re
from typing import Dict, List, Tuple
import gensim.corpora as corpora
from pyLDAvis import gensim_models
import pickle 
import pyLDAvis
import os
import gensim
from pprint import pprint

def vis(num_topics, corpus, id2word, lda_model):
    LDAvis_data_filepath = os.path.join('./results/ldavis_prepared_'+str(num_topics))

    # # this is a bit time consuming - make the if statement True
    # # if you want to execute visualization prep yourself
    if 1 == 1:
        LDAvis_prepared = gensim_models.prepare(lda_model, corpus, id2word)
        with open(LDAvis_data_filepath, 'wb') as f:
            pickle.dump(LDAvis_prepared, f)

    # load the pre-prepared pyLDAvis data from disk
    with open(LDAvis_data_filepath, 'rb') as f:
        LDAvis_prepared = pickle.load(f)

    pyLDAvis.save_html(LDAvis_prepared, './results/ldavis_prepared_'+ str(num_topics) +'.html')

def train(corpus, id2word, num_topics):
    #num_topics = 10

    # Build LDA model
    # print("corpus", corpus)
    # print("id2word", id2word)
    # print("numOftopics", num_topics)
    lda_model = gensim.models.LdaMulticore(corpus=corpus, id2word=id2word,num_topics=num_topics)
    # Print the Keyword in the 10 topics
    #pprint(lda_model.print_topics())
    # doc_lda = lda_model[corpus]
    #vis(num_topics, corpus, id2word, lda_model)
    return lda_model.print_topics()

def runModel(data_words, num_topics=10):    
    id2word = corpora.Dictionary(data_words)
    texts = data_words
    corpus = [id2word.doc2bow(text) for text in texts]
    #print(corpus[:1][0][:30])
    return train(corpus, id2word, num_topics)


def updatePercentageOnWord(weight : float, word: str, ner_labels :  List[Tuple[str, str]]) -> float:
    for ner_label in ner_labels:
        if ner_label[1] == 'PERSON':
            continue
        if word.lower() == ner_label[0].lower():
            return round(weight * (1 + (20 / 100)), 3)
    return weight
    
def parse_topic_string_and_update_percentage(topic_str: str, ner_labels : List[Tuple[str, str]]) -> List[Tuple[float, str]]:
    pattern = re.compile(r"([0-9.]+)\*\"([a-zA-Z]+)\"")
    substrings = topic_str.split("+")
    topic_tuples = []
    for substring in substrings:
        match = pattern.search(substring)
        if match:
            weight = float(match.group(1))
            word = match.group(2)
            updatedWeight = updatePercentageOnWord(weight, word, ner_labels)
            topic_tuples.append((updatedWeight, word))
    return topic_tuples

def updatePercentage(model_topics : List[Tuple[int, List[Tuple[str, float]]]], ner_labels : List[Tuple[str, str]]) -> List[List[Tuple[float, str]]]:
    # model topics : [(0, '0.220*"cau" + 0.134*"ff" + 0.090*"hh" + 0.090*"kkk" + ... '),
    # ner_labels : [('Tesr', 'ORG'), ...
    listOfTopics : Dict[int, List[Tuple[float, str]]] = {}
    for model_topic in model_topics:
        listOfTopics[model_topic[0]] = parse_topic_string_and_update_percentage(model_topic[1], ner_labels)
    return listOfTopics