from typing import List
import re
import gensim
from gensim.utils import simple_preprocess
import nltk
nltk.download('stopwords')
nltk.download('wordnet')
nltk.download('averaged_perceptron_tagger')
from nltk.corpus import stopwords, wordnet
from nltk.stem import WordNetLemmatizer
import pandas as pd
import en_core_web_sm

stop_words = stopwords.words('english')
stop_words.extend(['from', 'subject', 're', 'edu', 'use'])

def sent_to_words(sentences):
    for sentence in sentences:
        # deacc=True removes punctuations
        yield(gensim.utils.simple_preprocess(str(sentence), deacc=True))

def remove_stopwords(texts):
    return [[word for word in simple_preprocess(str(doc)) 
             if word not in stop_words] for doc in texts]

def clean(df: pd.DataFrame):
    df['text'].map(lambda x: re.sub('[,\.!?]', '', x))
    df['text'].map(lambda x: x.lower())
    return df

def nltk_pos_tagger(nltk_tag):
    if nltk_tag.startswith('J'):
        return wordnet.ADJ
    elif nltk_tag.startswith('V'):
        return wordnet.VERB
    elif nltk_tag.startswith('N'):
        return wordnet.NOUN
    elif nltk_tag.startswith('R'):
        return wordnet.ADV
    else:          
        return wordnet.NOUN

def ner(data):
    nlp = en_core_web_sm.load()
    nlp.get_pipe('ner').labels
    doc = nlp(data)
    return [(X.text, X.label_) for X in doc.ents]

def get_ner_labels(df: pd.DataFrame):
    return ner(','.join(list(df['text'].values)))

def lemmatize(words, wl:WordNetLemmatizer):
    nltk_tagged = nltk.pos_tag(words)  
    wordnet_tagged = map(lambda x: (x[0], nltk_pos_tagger(x[1])), nltk_tagged)
    lemmatized_sentence = []

    for word, tag in wordnet_tagged:
        #print("WORD " + word + " TAG " + tag)
        if tag is None:
            lemmatized_sentence.append(word)
        else:        
            lemmatized_sentence.append(wl.lemmatize(word, tag))
    return lemmatized_sentence

def preprocess(data_words):
    data_words = remove_stopwords(data_words)
    wl = WordNetLemmatizer()
    data_words = [lemmatize(words, wl) for words in data_words]
    return data_words

def process(m: List[str]):
    df = pd.DataFrame(m, columns =['text'])
    df = clean(df)
    ner_labels = get_ner_labels(df)
    data = df['text'].values.tolist()
    data_words = list(sent_to_words(data))
    data_words = preprocess(data_words)
    return data_words, ner_labels