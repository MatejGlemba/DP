package io.dpm.dropmenote.ws.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
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

    @PreDestroy
    public void close() throws InterruptedException {
        LOG.debug("Kafka producer is shutting down.");
        kafkaProducer.close();
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

    public static class USER_DATA implements INPUT_DATA {
        private String userID;
        private String notes;

        public USER_DATA(String userID, String notes) {
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

    public enum TOPIC {
        MESSAGE_DATA, USER_DATA, ROOM_DATA
    }
}
