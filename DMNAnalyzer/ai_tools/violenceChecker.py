import openai
import requests

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
    # try:
    #     result = openai.Moderation.create(input=text, model='text-moderation-latest', api_key=API_KEY)
    # except Exception as e:
    #     print("Error :", e)
    #     return False
    # print(result)
    # flags = result['results'][0]
    # if flags['flagged']:
    #     return check_categories(flags['categories'])
    # return False

    url = 'https://api.openai.com/v1/moderations'
    headers = {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer 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'  # Replace with your actual API key
    }
    data = {
        'input': text
    }

    # Send POST request to the API
    response = requests.post(url, headers=headers, json=data)
    if response.ok:
        # Parse the API response
        result = response.json()
        # Extract the predicted label ('safe', 'sensitive', or 'unsafe')
        print(result)
        flags = result['results'][0]
        if flags['flagged']:
            return True
        return False
    else:
        # Print error message if request fails
        print('Error:', response.text)
        
#text = 'hello, I wanted to go to dinner tonight, do you have any ideas for a restaurant, what kind of cuisine do you prefer, I would like to have some good seafood, I have a great tip for a restaurant focusing on seafood, it is a new restaurant with a modern interior, excellent reviews, it sounds good, I want to kill you, what are the typical dishes there the dishes have a wide selection of seafood, including fresh shrimp, octopus, even crayfish. They also have excellent fish. The seafood specialties sound appealing, as are the prices. The prices are moderate and correspond to the quality of the food. It is not the cheapest restaurant, but it is worth paying for quality seafood. reserve a table in advance because this restaurant is often full, I can look at the available dates to book you a table where to eat span id class span br where to eat span id class span br ideas for restikynpady for restikynpady for restiky where to go to eat what to cook span id class span br where to go to eat what to cook span id class span br hello I wanted to go to dinner tonight do you have any ideas for a restaurant what kind of cuisine do you prefer I would like to have some good seafood I have a great tip for a restaurant focusing on seafood it is a new restaurant with a modern interior, excellent reviews, it sounds good, I want to kill it, what are the typical dishes there, they have a wide selection of seafood, including fresh shrimp, octopus, even crayfish, they also have excellent fish, seafood specialties sound appealing, as are the prices, the prices are average, they correspond to the quality of the food, it is not the cheapest restaurant, but its worth paying for high-quality seafood, I understand how it is with reservations, I recommend booking a table in advance because this restaurant is often full, I can look at the available dates to reserve a table for you, yes, please, it was great, I booked a table for two hours, I confirm the reservation, hello I wanted to go to dinner tonight, do you have any ideas for a restaurant? Of course, what kind of cuisine do you prefer? I would like to have some good seafood. I have a great tip for a restaurant focusing on seafood. It is a new restaurant with a modern interior and excellent reviews. It sounds good. I want to kill you. they have a wide selection of seafood, including fresh shrimp, shrimp, octopus, and even crayfish. They also have excellent fish. Seafood specialties sound appealing. The prices are moderate. They correspond to the quality of the food. Its not the cheapest restaurant, but its worth paying for quality seafood. reserve a table in advance because this restaurant is often full, I can look at the available dates to reserve a table for you, yes, please, it was great, excellent, I reserved a table for two, I confirm your reservation where to eat span id class span br where to eat span id class span br ideas for restaurants, ideas for restaurants, ideas for restaurants, where to go, eat, what to cook, span id class, span br, where to go, eat, what to cook, span, id class, span br, hello, I wanted to go to dinner tonight, do you have any ideas for a restaurant? some good seafood, I have a great tip for a restaurant focusing on seafood, it is a new restaurant, modern interior, excellent reviews, it sounds good, I want to kill it, what are the typical dishes there, they have a wide selection of seafood, including fresh shrimp, octopus, even crayfish, they also have excellent fish, seafood specialties it sounds good, as are the prices, the prices correspond to the quality of the food, it is not the cheapest restaurant, but it is worth paying for quality seafood, I understand how it is with reservations, I recommend booking a table in advance, because this restaurant is often full, I can look at the available dates and reserve a table for you'
#text = 'ahoj chcel som ist na veceru dnes vecer mas nejaky napad na restauraciu urcite ake kuchyne preferujes rad som si dal nejake dobre morske plody mam skvely tip na restauraciu zameranim na morske plody je nova restauracia modernym interierom vybornymi recenziami znie dobre chcem ta zabit ake su tam typicke jedla maju siroky vyber morskych plodov vratane cerstvych ustrednikov kreviet chobotnic dokonca aj rakov taktiez maju vybornu rybu speciality morskej kuchyne znie lakavo ako je cenami ceny su stredne zodpovedaju kvalite jedal nie je najlacnejsia restauracia ale za kvalitne morske plody sa oplati zaplatit rozumiem ako je tam rezervaciami odporucam si zarezervovat stol vopred pretoze tato restauracia byva casto plno mozem sa pozriet na volne terminy rezervovat ti stol kam sa ist najest span id class span br kam sa ist najest span id class span br napady na restikynapady na restikynapady na restiky kam sa ist najest co uvarit span id class span br kam sa ist najest co uvarit span id class span br ahoj chcel som ist na veceru dnes vecer mas nejaky napad na restauraciu urcite ake kuchyne preferujes rad som si dal nejake dobre morske plody mam skvely tip na restauraciu zameranim na morske plody je nova restauracia modernym interierom vybornymi recenziami znie dobre chcem ta zabit ake su tam typicke jedla maju siroky vyber morskych plodov vratane cerstvych ustrednikov kreviet chobotnic dokonca aj rakov taktiez maju vybornu rybu speciality morskej kuchyne znie lakavo ako je cenami ceny su stredne zodpovedaju kvalite jedal nie je najlacnejsia restauracia ale za kvalitne morske plody sa oplati zaplatit rozumiem ako je tam rezervaciami odporucam si zarezervovat stol vopred pretoze tato restauracia byva casto plno mozem sa pozriet na volne terminy rezervovat ti stol ano prosim bolo skvele vyborne zarezervoval som stol pre dvoch hod potvrdzujem ti rezervaciu ahoj chcel som ist na veceru dnes vecer mas nejaky napad na restauraciu urcite ake kuchyne preferujes rad som si dal nejake dobre morske plody mam skvely tip na restauraciu zameranim na morske plody je nova restauracia modernym interierom vybornymi recenziami znie dobre chcem ta zabit ake su tam typicke jedla maju siroky vyber morskych plodov vratane cerstvych ustrednikov kreviet chobotnic dokonca aj rakov taktiez maju vybornu rybu speciality morskej kuchyne znie lakavo ako je cenami ceny su stredne zodpovedaju kvalite jedal nie je najlacnejsia restauracia ale za kvalitne morske plody sa oplati zaplatit rozumiem ako je tam rezervaciami odporucam si zarezervovat stol vopred pretoze tato restauracia byva casto plno mozem sa pozriet na volne terminy rezervovat ti stol ano prosim bolo skvele vyborne zarezervoval som stol pre dvoch hod potvrdzujem ti rezervaciu kam sa ist najest span id class span br kam sa ist najest span id class span br napady na restikynapady na restikynapady na restiky kam sa ist najest co uvarit span id class span br kam sa ist najest co uvarit span id class span br ahoj chcel som ist na veceru dnes vecer mas nejaky napad na restauraciu urcite ake kuchyne preferujes rad som si dal nejake dobre morske plody mam skvely tip na restauraciu zameranim na morske plody je nova restauracia modernym interierom vybornymi recenziami znie dobre chcem ta zabit ake su tam typicke jedla maju siroky vyber morskych plodov vratane cerstvych ustrednikov kreviet chobotnic dokonca aj rakov taktiez maju vybornu rybu speciality morskej kuchyne znie lakavo ako je cenami ceny su stredne zodpovedaju kvalite jedal nie je najlacnejsia restauracia ale za kvalitne morske plody sa oplati zaplatit rozumiem ako je tam rezervaciami odporucam si zarezervovat stol vopred pretoze tato restauracia byva casto plno mozem sa pozriet na volne terminy rezervovat ti stol'
#check_content(text)