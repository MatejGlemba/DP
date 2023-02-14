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

def train(corpus, id2word):
    num_topics = 10

    # Build LDA model
    lda_model = gensim.models.LdaMulticore(corpus=corpus, id2word=id2word,num_topics=num_topics)
    # Print the Keyword in the 10 topics
    pprint(lda_model.print_topics())
    # doc_lda = lda_model[corpus]
    vis(num_topics, corpus, id2word, lda_model)
    return lda_model

def runModel(data_words):    
    id2word = corpora.Dictionary(data_words)
    texts = data_words
    corpus = [id2word.doc2bow(text) for text in texts]
    print(corpus[:1][0][:30])
    return train(corpus, id2word)