from multiprocessing import Process
import time
from confluent_kafka import Consumer, KafkaError

# Kafka configuration
conf = {
    'bootstrap.servers': 'localhost:9094',  # Replace with the appropriate Kafka broker address
    'group.id': 'foo',
    'auto.offset.reset': 'earliest'
}


def main():
    # Create Kafka consumer instance
    consumer = Consumer(conf)

    # Subscribe to the 'test' topic
    consumer.subscribe(['ROOM_DATA','MESSAGE_DATA'])

    # Poll for new messages
    while True:
        msg = consumer.poll(1.0)
        #time.sleep(1)
        if msg is None:
            print("nothing incoming")
            continue
        if msg.error():
            if msg.error().code() == KafkaError._PARTITION_EOF:
                print('Reached end of partition')
            else:
                print('Error while polling for messages: {}'.format(msg.error()))
        else:
            print('Received message: key={}, value={}, topic={}, partition={}, offset={}'.format(
                msg.key(), msg.value(), msg.topic(), msg.partition(), msg.offset()
            ))

def main2():
    # Create Kafka consumer instance
    consumer = Consumer(conf)

    # Subscribe to the 'test' topic
    consumer.subscribe(['MESSAGE_DATA'])

    # Poll for new messages
    while True:
        msg = consumer.poll(1.0)
        time.sleep(1)
        if msg is None:
            continue
        if msg.error():
            if msg.error().code() == KafkaError._PARTITION_EOF:
                print('Reached end of partition')
            else:
                print('Error while polling for messages: {}'.format(msg.error()))
        else:
            print('Received message: key={}, value={}, topic={}, partition={}, offset={}'.format(
                msg.key(), msg.value(), msg.topic(), msg.partition(), msg.offset()
            ))


if __name__ == "__main__":
    main()
