package io.dpm.dropmenote.ws.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
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
        LOG.debug("Kafka producer is initialized.");
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
        Properties config = new Properties();
        config.put("client.id", CLIENT_ID);
        config.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        config.put("acks", ACKS);
        config.put("group.id", "foo");
        this.kafkaTopicConsumer = new KafkaTopicConsumerLoop(new KafkaConsumer<K, V>(config), topicConsumer);
        new Thread(kafkaTopicConsumer).start();
    }

    @PreDestroy
    public void close() throws InterruptedException {
        LOG.debug("Kafka producer is shutting down.");
        kafkaProducer.close();
        kafkaTopicConsumer.shutdown();
    }

    private class KafkaTopicConsumerLoop implements Runnable {
        private static final String TOPIC = "message_outputs";
        private final KafkaConsumer<K, V> kafkaConsumer;
        private final CountDownLatch shutdownLatch;
        private final List<String> topics;
        private final Consumer<MESSAGE_OUTPUT> topicConsumer;

        public KafkaTopicConsumerLoop(final KafkaConsumer<K,V> consumer,
                                      final Consumer<MESSAGE_OUTPUT> topicConsumer) {
            this.kafkaConsumer = consumer;
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
                    ConsumerRecords<K, V> records = kafkaConsumer.poll(Duration.ofSeconds(Long.MAX_VALUE));
                    records.forEach(record -> topicConsumer.accept(deserialize(record.value())));
                }

            } catch (WakeupException ex) {
                // ignore
            } catch (Exception e) {
                LOG.error("Unexpected error", e);
            } finally {
                kafkaConsumer.close();
                shutdownLatch.countDown();
            }
        }

        private MESSAGE_OUTPUT deserialize(V value) {
            try {
                return new ObjectMapper().readValue((String) value, MESSAGE_OUTPUT.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        public void shutdown() throws InterruptedException {
            LOG.debug("Kafka consumer is shutting down.");
            kafkaConsumer.wakeup();
            shutdownLatch.await();
        }
    }

    public interface INPUT_DATA{}

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
        private String roomID;
        private String qrcodeID;
        private String userID;
        private String notes;

        public BLACKLIST_DATA(String roomID, String qrcodeID, String userID, String notes) {
            this.roomID = roomID;
            this.qrcodeID = qrcodeID;
            this.userID = userID;
            this.notes = notes;
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

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class IMAGE_DATA implements INPUT_DATA {
        private String qrcodeID;
        private DataBufferByte image;

        public IMAGE_DATA(String qrcodeID, DataBufferByte image) {
            this.qrcodeID = qrcodeID;
            this.image = image;
        }

        public String getQrcodeID() {
            return qrcodeID;
        }

        public void setQrcodeID(String qrcodeID) {
            this.qrcodeID = qrcodeID;
        }

        public DataBufferByte getImage() {
            return image;
        }

        public void setImage(DataBufferByte image) {
            this.image = image;
        }
    }
    public static class MESSAGE_OUTPUT {
        private String roomID;
        private String qrcodeID;
        private String userID;
        private NOTIFICATION_TYPE notification_type;

        public MESSAGE_OUTPUT(String roomID, String qrcodeID, String userID, String notification_type) {
            this.roomID = roomID;
            this.qrcodeID = qrcodeID;
            this.userID = userID;
            this.notification_type = NOTIFICATION_TYPE.valueOf(notification_type);
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

        public void setNotification_type(String notification_type) {
            this.notification_type = NOTIFICATION_TYPE.valueOf(notification_type);
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

        public NOTIFICATION_TYPE getNotification_type() {
            return notification_type;
        }
    }

    public enum NOTIFICATION_TYPE {
        SPAM, HATE
    }

    public enum TOPIC {
        MESSAGE_DATA, BLACKLIST_DATA
    }
}
