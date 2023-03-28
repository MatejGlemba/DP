import re

# define a function to check if a given text is spam
def checkSpam(text: str):
    # define a regular expression pattern for detecting common spam phrases
    pattern = re.compile(r'(buy now|click here|limited time offer|make money fast)', re.IGNORECASE)
    
    # search for the pattern in the input text
    match = pattern.search(text)
    
    # if a match is found, return True (spam)
    if match:
        return True
    
    # if no match is found, return False (not spam)
    return False

