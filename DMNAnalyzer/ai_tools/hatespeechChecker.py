import re

# define a list of popular swear words and their variations
swear_words = [
    "fuck",
    "fker",
    "f*ck",
    "f**k",
    "shit",
    "sh*t",
    "s*it",
    "sh!t",
    "asshole",
    "a**hole",
    "a*hole",
    "a-hole",
    "as$hole",
    "a$$hole",
    "bitch",
    "b*tch",
    "b!tch",
    "dick",
    "d*ck",
    "d!ck",
    "motherfucker",
    "motherfker",
    "motherf**ker",
    "pussy",
    "pu**y",
    "pus$y",
    "pu$sy",
    "pu$$y",
    "cock",
    "cOck",
    "c*ck",
    "c0ck",
    "ass",
    "as$",
    "a$s",
    "a$$",
    "as*",
    "a*s",
    "a**",
    "cunt",
    "c*nt",
    "c**t",
    "bastard",
    "twat",
    "wanker",
    "prick",
    "pr*ck",
    "pr!ck",
    "bollocks",
    "Douchebag",
    "doucheb@g",
    'piča', 
    'p*ča', 
    'hovno',
    'h**no',
    'hOvno',
    'hovnO',
    'hovn0',
    'h0vno', 
    'kokot',
    'k0k0t',
    'k*k*t',
    'kokot',  
    'debil',
    'deb*l',
    'deb!l', 
    'kretén', 
    'kurva',
    'k*rva',
    'k*rvenec',
    'k**venec', 
    'suka',
    's*ka'
]


# define a function to match swear words in a string
def checkHate(text: str):
    # compile a regular expression pattern with the swear words and their variations
    pattern = re.compile(r"\b(" + "|".join([re.escape(w) for w in swear_words]) + r")(s|ed|ing|er)?\b", re.IGNORECASE)
    
    # search for the pattern in the input text
    match = pattern.search(text)

    # if a match is found, return True
    if match:
        return True
    
    # otherwise, return False
    else:
        return False
