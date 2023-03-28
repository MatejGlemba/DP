from typing import List
import re
from wordcloud import WordCloud
import gensim
from gensim.utils import simple_preprocess
import nltk
nltk.download('stopwords')
nltk.download('wordnet')
nltk.download('averaged_perceptron_tagger')
from nltk.corpus import stopwords, wordnet
from nltk.stem import WordNetLemmatizer
import pandas as pd
from pprint import pprint
import matplotlib.pyplot as plt
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

# def lemmatizer(string):
#     word_pos_tags = nltk.pos_tag(word_tokenize(string)) # Get position tags
#     a=[wl.lemmatize(tag[0], get_wordnet_pos(tag[1])) for idx, tag in enumerate(word_pos_tags)] # Map the position tag and lemmatize the word/token
#     return " ".join(a)

def ner(data):
    nlp = en_core_web_sm.load()
    nlp.get_pipe('ner').labels
    doc = nlp(data)
    #print("NER")
    #print([(X.text, X.label_) for X in doc.ents])
    return [(X.text, X.label_) for X in doc.ents]

def eda(df: pd.DataFrame):
    long_string = ','.join(list(df['text'].values))
   # print("LONG STRING EDA", long_string)
    # wordcloud = WordCloud(background_color="white", max_words=1000, contour_width=3, contour_color='steelblue')
    # wc = wordcloud.generate(long_string)
    
    


    # plt.figure()
    # plt.imshow(wc, interpolation='bilinear')
    # plt.axis("off")
    # plt.savefig('foo.png')

    return ner(long_string)


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
   # print("DATA WORDS without stopwords", data_words)
    #st = PorterStemmer()
    #data_words = [[st.stem(word) for word in words ] for words in data_words]
    #print("DATA WORDS after stemming", data_words)
    wl = WordNetLemmatizer()
    #print("\n")
    data_words = [lemmatize(words, wl) for words in data_words]
    
    #data_words_POS = [[nltk.pos_tag(word) for word in words ] for words in data_words]
    #data_words_wordnet_POS = map(lambda x: (x[0], nltk_pos_tagger(x[1])), data_words_POS)
    #data_words = [[wl.lemmatize(word) for word in words ] for words in data_words]
    #print("DATA WORDS after lemmatizing", data_words)
    return data_words

    
def process(m: List[str]):
    df = pd.DataFrame(m, columns =['text'])
    df = clean(df)
    ner_labels = eda(df)
    data = df['text'].values.tolist()
    data_words = list(sent_to_words(data))
    #print("DATA WORDS before preprocessing", data_words)
    data_words = preprocess(data_words)
    #print("DATA WORDS after preprocessing", data_words)
    return data_words, ner_labels

