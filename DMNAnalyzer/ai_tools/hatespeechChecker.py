import re

# define a list of popular swear words and their variations
swear_words = [
    "fuck",
    "shit",
    "asshole",
    "bitch",
    "dick",
    "motherfucker",
    "pussy",
    "cock",
    "ass",
    "cunt",
    "bastard",
    "twat",
    "wanker",
    "prick",
    "fucker",
    "bollocks"
]

# define a function to match swear words in a string
def checkHate(text: str):
    # compile a regular expression pattern with the swear words and their variations
    pattern = re.compile(r"\b(" + "|".join([re.escape(w) for w in swear_words]) + r")(s|ed|ing|er)?\b", re.IGNORECASE)
    
    # search for the pattern in the input text
    match = pattern.search(text)
    print("CHECK HATE ", text)
    print("CHECK HATE results ", match)

    # if a match is found, return True
    if match:
        return True
    
    # otherwise, return False
    else:
        return False
