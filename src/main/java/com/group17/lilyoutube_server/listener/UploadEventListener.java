package com.group17.lilyoutube_server.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.group17.lilyoutube_server.config.RabbitConfig;
import com.group17.lilyoutube_server.dto.UploadEventDTO;
import com.group17.lilyoutube_server.proto.UploadEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UploadEventListener {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.QUEUE_JSON)
    public void receiveJsonMessage(byte[] message) {
        long startTime = System.nanoTime();
        try {
            UploadEventDTO event = objectMapper.readValue(message, UploadEventDTO.class);
            long endTime = System.nanoTime();
            double deserializationTime = (endTime - startTime) / 1000.0;
            
            log.info("Received JSON UploadEvent: id={}, title={}, user={}, deserialization time={} µs",
                    event.getId(), event.getTitle(), event.getUsername(), deserializationTime);
        } catch (Exception e) {
            log.error("Failed to parse JSON UploadEvent", e);
        }
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_PROTO)
    public void receiveProtoMessage(byte[] message) {
        long startTime = System.nanoTime();
        try {
            UploadEvent event = UploadEvent.parseFrom(message);
            long endTime = System.nanoTime();
            double deserializationTime = (endTime - startTime) / 1000.0;
            
            log.info("Received Protobuf UploadEvent: id={}, title={}, user={}, deserialization time={} µs",
                    event.getId(), event.getTitle(), event.getUsername(), deserializationTime);
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse Protobuf UploadEvent", e);
        }
    }
}