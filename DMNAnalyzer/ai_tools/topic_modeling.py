import re
from typing import Dict, List, Tuple
import gensim.corpora as corpora
import gensim

def train(corpus, id2word, num_topics, num_words, passes, iterations, minimum_probability):
    lda_model = gensim.models.LdaMulticore(corpus=corpus, id2word=id2word, num_topics=num_topics, passes=passes , iterations=iterations, minimum_probability=minimum_probability)
    return lda_model.print_topics(num_words=num_words)

def runModel(documents, num_topics=10, num_words=7, passes=5, iterations=50, minimum_probability=0.05):    
    # data_words - list of (int, list of (str, float))
    id2word = corpora.Dictionary(documents)
    corpus = [id2word.doc2bow(document) for document in documents]
    return train(corpus, id2word, num_topics, num_words, passes, iterations, minimum_probability)

def updatePercentageOnWordByNerLabel(weight : float, word: str, ner_labels :  List[Tuple[List[str], str]]) -> float:
    for tokens, label in ner_labels:
        if label in ['NORP', 'ORG', 'GPE', 'LOC', 'PRODUCT', 'EVENT', 'WORK_OF_ART']:
            if word.lower() in tokens:
                newWeight = round(weight * (1 + (50 / 100)), 3) # add 50%
          #      print("\nNER update word {} : old {} new {}".format(word, weight, newWeight))
                return newWeight
    return weight

def updatePercentageOnWordByTopWords(weight : float, word: str, topWords : List[str]) -> float:
    if word.lower() in topWords:
        newWeight = round(weight * (1 + (90 / 100)), 3) # add 90%
   #     print("\nTOP update word {} : old {} new {}".format(word, weight, newWeight))
        return newWeight
    return weight
    
def parse_topic_string_and_update_percentage(topic_str: str, ner_labels : List[Tuple[List[str], str]], topWords : List[str]) -> List[Tuple[float, str]]:
    pattern = re.compile(r"([0-9.]+)\*\"([a-zA-Z]+)\"")
    substrings = topic_str.split("+")
    topic_tuples = []
    for substring in substrings:
        match = pattern.search(substring)
        if match:
            weight = float(match.group(1))
            word = match.group(2)
            if topWords:
                weight = updatePercentageOnWordByTopWords(weight, word, topWords)
            if ner_labels:
                weight = updatePercentageOnWordByNerLabel(weight, word, ner_labels)
        
            topic_tuples.append((weight, word))
    return topic_tuples

def updatePercentage(model_topics : List[Tuple[int, List[Tuple[str, float]]]], ner_labels : List[Tuple[List[str], str]] = None, topWords : List[str] = None) -> List[List[Tuple[float, str]]]:
    # model topics : [(0, '0.220*"cau" + 0.134*"ff" + 0.090*"hh" + 0.090*"kkk" + ... '),
    # ner_labels : [('Tesr', 'ORG'), ...
    listOfTopics : Dict[int, List[Tuple[float, str]]] = {}
    for model_topic in model_topics:
        listOfTopics[model_topic[0]] = parse_topic_string_and_update_percentage(model_topic[1], ner_labels, topWords)
    return listOfTopics