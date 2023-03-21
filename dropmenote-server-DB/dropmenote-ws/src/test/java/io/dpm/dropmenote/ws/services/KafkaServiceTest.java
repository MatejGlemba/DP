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
        KafkaService.MESSAGE_OUTPUT messageOutput1 = new KafkaService.MESSAGE_OUTPUT("sadfads", "sfsadfa", "afads", "HATE");
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String s;
        try {
            s = objectWriter.writeValueAsString(messageOutput1);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Assert.assertTrue(s != null);
        String json = "{\"roomID\": \"!DaUcqPSGUDWyGONNJQ:matrix.dropmenote.com\", \"qrcodeID\": \"axAK2s300AFUXR8zfBMw\", \"userID\": \"8FfCO6NPQKwi0rEBg35LjkX874iicQlwiOXWNMTXj2iUZ1vf0c\", \"type\": \"HATE\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        String s1 = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(json));
        KafkaService.MESSAGE_OUTPUT messageOutput = new ObjectMapper().readValue(s1, KafkaService.MESSAGE_OUTPUT.class);
        Assert.assertTrue(messageOutput != null);
    }
}