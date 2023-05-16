from typing import List, Tuple
import re
from gensim.utils import simple_preprocess
import nltk
nltk.download('stopwords')
nltk.download('wordnet')
nltk.download('averaged_perceptron_tagger')
from nltk.corpus import stopwords, wordnet
from nltk.stem import WordNetLemmatizer
import en_core_web_sm

stop_words = stopwords.words('english')
stop_words.extend(['from', 'subject', 're', 'edu', 'use', 'yes', 'no', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten'])

def covert_to_tokens(sentences, punctuations=True, min_len=3, max_len=15):
    for sentence in sentences:
        # deacc=True removes punctuations
        yield(simple_preprocess(str(sentence), deacc=punctuations, min_len=min_len, max_len=max_len))

def remove_stopwords(texts):
    return [[word for word in doc if word not in stop_words] for doc in texts]

def nltk_pos_tagger(nltk_tag, blacklist):
    if blacklist:
        if nltk_tag.startswith('J'):
            return wordnet.ADJ
        elif nltk_tag.startswith('V'):
            return wordnet.VERB
        elif nltk_tag.startswith('N'):
            return wordnet.NOUN
        elif nltk_tag.startswith('R'):
            return wordnet.ADV
        else:          
            return None
    else:
        if nltk_tag.startswith('N'):
            return wordnet.NOUN
        else:
            return None

def ner(data):
    nlp = en_core_web_sm.load()
    nlp.get_pipe('ner').labels
    doc = nlp(data)
    return [(simple_preprocess(X.text), X.label_) for X in doc.ents]

def lemmatize(words, wl:WordNetLemmatizer, forBlacklist):
    nltk_tagged = nltk.pos_tag(words)  
    wordnet_tagged = map(lambda x: (x[0], nltk_pos_tagger(x[1], forBlacklist)), nltk_tagged)
    lemmatized_sentence = []

    for word, tag in wordnet_tagged:
        if tag is None:
            pass
            #lemmatized_sentence.append(word)
        else:        
            lemmatized_sentence.append(wl.lemmatize(word, tag))
    return lemmatized_sentence

def preprocess(sentencesInTokens, forBlacklist):
    sentencesInTokens = remove_stopwords(sentencesInTokens)
    wl = WordNetLemmatizer()
    sentencesInTokens = [lemmatize(words, wl, forBlacklist) for words in sentencesInTokens]
    return sentencesInTokens

def remove_names(sentencesInTokens : List[List[str]], ner_labels: List[Tuple[List[str], str]]):
    personNerLabels = []
    for tokens, label in ner_labels:
        if label == 'PERSON':
            personNerLabels.extend(tokens)

    return [[token for token in sentenceInTokens if token not in personNerLabels] for sentenceInTokens in sentencesInTokens]

def process(sentences: List[str], addNerLabels: True, forBlacklist: False):
    sentencesWithoutSpecialChars = [re.sub('[,\.!?]', '', sentence) for sentence in sentences]
    if addNerLabels:
        one_document = ' '.join(sentencesWithoutSpecialChars)
        ner_labels = ner(one_document)

    sentencesInTokens = list(covert_to_tokens(sentencesWithoutSpecialChars))
    if addNerLabels:
        sentencesInTokens = remove_names(sentencesInTokens, ner_labels)

    sentencesInTokens = preprocess(sentencesInTokens, forBlacklist)
    if addNerLabels:
        return sentencesInTokens, ner_labels
    else:
        return sentencesInTokens