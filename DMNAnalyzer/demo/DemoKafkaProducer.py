import json
from time import sleep
from confluent_kafka import Producer


KAFKA_BOOTSTRAP_SERVERS_CONS = "localhost:9094"
KAFKA_INPUT_TOPIC_NAME_CONS = "inputmallstream"

producer = Producer({'bootstrap.servers' : KAFKA_BOOTSTRAP_SERVERS_CONS})
roomID = 0
qrchodeID = 0
with open('text.txt', mode="r") as f:
    for line in f:
        message = {
            "roomID" : roomID,
            "qrcodeID" : qrchodeID,
            "data" : line
        }
        message = json.dumps(message)
        print(message)
        roomID+=1
        qrchodeID+=1
        sleep(5)
        producer.produce(KAFKA_INPUT_TOPIC_NAME_CONS, message)
        producer.poll(0)
        producer.flush()


