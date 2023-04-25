from multiprocessing import Process
from confluent_kafka import Producer

# Kafka configuration
conf = {
    'bootstrap.servers': 'localhost:9094',  # Replace with the appropriate Kafka broker address
    'client.id': 'producer-client'
}

# Callback function for delivery reports
def delivery_report(err, msg):
    if err is not None:
        print('Message delivery failed: {}'.format(err))
    else:
        print('Message delivered to {} [{}]'.format(msg.topic(), msg.partition()))

def main():
# Create Kafka producer instance
    producer = Producer(conf)

    for a in range(5):
    # Produce a message to the 'test' topic
        producer.produce('MESSAGE_DATA', key='', value='value' + str(a), callback=delivery_report)

    # Wait for any outstanding messages to be delivered and delivery reports to be received
    producer.flush()

def main2():
# Create Kafka producer instance
    producer = Producer(conf)

    for a in range(5):
    # Produce a message to the 'test' topic
        producer.produce('ROOM_DATA', key='', value='haha' + str(a), callback=delivery_report)

    # Wait for any outstanding messages to be delivered and delivery reports to be received
    producer.flush()

if __name__ == "__main__":
    p1 = Process(target=main)
    p1.start()
    p2 = Process(target=main2)
    p2.start()
    p1.join()
    p2.join()