package io.dpm.dropmenote.ws.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class KafkaServiceTest extends TestCase {

    @Test
    public void test() throws IOException {
        KafkaService.USER_DATA userData = new KafkaService.USER_DATA("sadfads", "HATE");
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String s;
        try {
            s = objectWriter.writeValueAsString(userData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Assert.assertTrue(s != null);
        String json = "{\"userID\": \"8FfCO6NPQKwi0rEBg35LjkX874iicQlwiOXWNMTXj2iUZ1vf0c\", \"notes\": \"HATE\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        String s1 = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(json));
        KafkaService.USER_DATA messageOutput = new ObjectMapper().readValue(s1, KafkaService.USER_DATA.class);
        Assert.assertTrue(messageOutput != null);
    }
}