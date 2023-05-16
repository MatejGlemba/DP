# Use Python 3.8.16 as the base image
FROM python:3.8.16

WORKDIR /app

# install necessary apt packages
RUN apt-get update && apt-get install -y libgl1-mesa-glx

# copy requirements for libs

COPY requirements.txt requirements.txt

# install libs
RUN pip3 install -r requirements.txt

# copy all source code from local machine into WORKDIR 
COPY . .

# download nlpk datased for NER labeling
RUN python3 -m spacy download en_core_web_sm

# Run the main.py script
CMD ["python", "main.py"]

