package io.dpm.dropmenote.ws.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KafkaService<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaService.class);

    @Value("${kafka.client.id}")
    private String CLIENT_ID;
    @Value("${kafka.bootstrap.servers}")
    private String BOOTSTRAP_SERVERS;
    @Value("${kafka.acks}")
    private String ACKS;
    private KafkaProducer<K, String> kafkaProducer;
    private KafkaConsumer<K, V> kafkaConsumer;
    private KafkaTopicConsumerLoop kafkaTopicConsumer;

    @PostConstruct
    public void init() {
        Properties config = new Properties();
        config.put("client.id", CLIENT_ID);
        config.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        config.put("acks", ACKS);
        config.put("group.id", "foo");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.kafkaProducer = new KafkaProducer<K, String>(config);
        config = new Properties();
        config.put("client.id", CLIENT_ID);
        config.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        config.put("acks", ACKS);
        config.put("group.id", "foo");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        this.kafkaConsumer = new KafkaConsumer<K, V>(config);
        LOG.debug("Kafka service is initialized.");
    }

    public void produce(final TOPIC topic, final INPUT_DATA value) {
        final ProducerRecord<K, String> record = new ProducerRecord<>(topic.name(), null, serializeToJson(value));
        kafkaProducer.send(record, (metadata, e) -> {
            if (e != null)
                LOG.warn("Send failed for record {}", record, e);
        });
    }

    private String serializeToJson(final INPUT_DATA value) {
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void startConsumerLoop(final Consumer<MESSAGE_OUTPUT> topicConsumer) {
        if (kafkaTopicConsumer == null) {
            this.kafkaTopicConsumer = new KafkaTopicConsumerLoop(topicConsumer);
            Thread t = new Thread(kafkaTopicConsumer);
            t.start();
        }
    }

    @PreDestroy
    public void close() throws InterruptedException {
        LOG.debug("Kafka producer is shutting down.");
        kafkaProducer.close();
        kafkaTopicConsumer.shutdown();
        kafkaTopicConsumer = null;
    }

    public void startProducer(TOPIC topic, List<INPUT_DATA> listOfInputData) {
        //Thread t = new Thread(() -> {
            for (INPUT_DATA inputData : listOfInputData) {
                produce(topic, inputData);
            }
          //  Thread.currentThread().interrupt();
        //});
        //t.start();
    }
    private class KafkaTopicConsumerLoop implements Runnable {
        private static final String TOPIC = "MESSAGE_OUTPUT";
        private final CountDownLatch shutdownLatch;
        private final List<String> topics;
        private Consumer<MESSAGE_OUTPUT> topicConsumer;

        public KafkaTopicConsumerLoop(final Consumer<MESSAGE_OUTPUT> topicConsumer) {
            this.shutdownLatch = new CountDownLatch(1);
            this.topicConsumer = topicConsumer;
            this.topics = Collections.singletonList(TOPIC);
        }

        @Override
        public void run() {
            try {
                kafkaConsumer.subscribe(topics);
                LOG.debug("Kafka consumer subscribed to topic {}", TOPIC);
                while (true) {
                    ConsumerRecords<K, V> records = kafkaConsumer.poll(Duration.ofSeconds(5));
                    records.forEach(record -> topicConsumer.accept(deserialize(record.value())));
                }

            } catch (WakeupException ex) {
                Thread.currentThread().interrupt();
                // ignore
            } catch (Exception e) {
                LOG.error("Unexpected error", e);
                //Thread.currentThread().interrupt();
            }
        }

        private MESSAGE_OUTPUT deserialize(V value) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree((String) value));
                return objectMapper.readValue(json, MESSAGE_OUTPUT.class);
            } catch (IOException e) {
                LOG.debug("Problem with deserialization {}", e);
                //throw new RuntimeException(e);
                return new MESSAGE_OUTPUT();
            }

        }
        public void shutdown() throws InterruptedException {
            LOG.debug("Kafka consumer is shutting down.");
            kafkaConsumer.wakeup();
            kafkaConsumer.close();
            shutdownLatch.await();
        }
    }

    public interface INPUT_DATA {}

    public static class MESSAGE_DATA implements INPUT_DATA {
        private String roomID;
        private String qrcodeID;
        private String userID;
        private String data;

        public MESSAGE_DATA(String roomID, String qrcodeID, String userID, String data) {
            this.roomID = roomID;
            this.qrcodeID = qrcodeID;
            this.userID = userID;
            this.data = data;
        }

        public String getRoomID() {
            return roomID;
        }

        public void setRoomID(String roomID) {
            this.roomID = roomID;
        }

        public String getQrcodeID() {
            return qrcodeID;
        }

        public void setQrcodeID(String qrcodeID) {
            this.qrcodeID = qrcodeID;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    public static class BLACKLIST_DATA implements INPUT_DATA {
        private String userID;
        private String notes;

        public BLACKLIST_DATA(String userID, String notes) {
            this.userID = userID;
            this.notes = notes;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
    public static class ROOM_DATA implements INPUT_DATA {
        private String qrcodeID;
        private String photoPath;
        private String description;
        private String roomName;

        public ROOM_DATA(String qrcodeID, String photoPath, String description, String roomName) {
            this.qrcodeID = qrcodeID;
            this.description = description;
            this.photoPath = photoPath;
            this.roomName = roomName;
        }

        public String getQrcodeID() {
            return qrcodeID;
        }

        public void setQrcodeID(String qrcodeID) {
            this.qrcodeID = qrcodeID;
        }

        public String getPhotoPath() {
            return photoPath;
        }

        public void setPhotoPath(String photoPath) {
            this.photoPath = photoPath;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getRoomName() {
            return roomName;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }
    }
    public static class MESSAGE_OUTPUT {
        private String roomID;
        private String qrcodeID;
        private String userID;
        private NOTIFICATION_TYPE type;

        public MESSAGE_OUTPUT() {}
        public MESSAGE_OUTPUT(String roomID, String qrcodeID, String userID, String type) {
            this.roomID = roomID;
            this.qrcodeID = qrcodeID;
            this.userID = userID;
            this.type = NOTIFICATION_TYPE.valueOf(type);
        }

        public void setRoomID(String roomID) {
            this.roomID = roomID;
        }

        public void setQrcodeID(String qrcodeID) {
            this.qrcodeID = qrcodeID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public void setType(String type) {
            this.type = NOTIFICATION_TYPE.valueOf(type);
        }

        public String getRoomID() {
            return roomID;
        }

        public String getQrcodeID() {
            return qrcodeID;
        }

        public String getUserID() {
            return userID;
        }

        public NOTIFICATION_TYPE getType() {
            return type;
        }
    }
    public enum NOTIFICATION_TYPE {
        SPAM, HATE
    }

    public enum TOPIC {
        MESSAGE_DATA, BLACKLIST_DATA, ROOM_DATA, MESSAGE_OUTPUT
    }
}
