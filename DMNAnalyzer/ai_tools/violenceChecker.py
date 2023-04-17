import openai

API_KEY = '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'
MODEL = 'text-moderation-latest'

def check_categories(categories):
    # {
    #     'id': 'modr-76GUIAzG3spZoX14XuMoYpVAZFTzE', 
    #     'model': 'text-moderation-004', 
    #     'results': [
    #         {
    #             'flagged': False, 
    #             'categories': {
    #                 'sexual': False, 
    #                 'hate': False, 
    #                 'violence': False, 
    #                 'self-harm': False, 
    #                 'sexual/minors': False, 
    #                 'hate/threatening': False, 
    #                 'violence/graphic': False
    #             }, 
    #             'category_scores': {
    #                 'sexual': 7.286330219358206e-05, 
    #                 'hate': 0.0005412251339294016, 
    #                 'violence': 5.478604361996986e-05, 
    #                 'self-harm': 5.6369672165601514e-06, 
    #                 'sexual/minors': 3.0630914693574596e-07, 
    #                 'hate/threatening': 8.810876295228809e-08, 
    #                 'violence/graphic': 1.003195848170435e-06
    #             }
    #         }
    #     ]
    # }

    # Iterate through the values in the 'categories' dictionary
    for flag in categories.values():
        # If any flag is True, return True
        if flag:
            return True
    # If no flag is True, return False
    return False

def check_content(text):
    try:
        result = openai.Moderation.create(input=text, model=MODEL, api_key=API_KEY)
    except Exception as e:
        print("Error :", e)
        return False
    #print(result)
    flags = result['results'][0]
    if flags['flagged']:
        return check_categories(flags['categories'])
    return False