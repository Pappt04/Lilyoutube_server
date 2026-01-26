package com.group17.lilyoutube_server.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group17.lilyoutube_server.config.RabbitConfig;
import com.group17.lilyoutube_server.dto.UploadEvent;
import com.group17.lilyoutube_server.proto.UploadEventProto;
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
        try {
            UploadEvent event = objectMapper.readValue(message, UploadEvent.class);
            log.info("Received JSON UploadEvent: id={}, title={}, user={}",
                    event.getId(), event.getTitle(), event.getUsername());
        } catch (Exception e) {
            log.error("Failed to parse JSON UploadEvent", e);
        }
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_PROTO)
    public void receiveProtoMessage(byte[] message) {
        try {
            com.group17.lilyoutube_server.proto.UploadEvent event = com.group17.lilyoutube_server.proto.UploadEvent
                    .parseFrom(message);
            log.info("Received Protobuf UploadEvent: id={}, title={}, user={}",
                    event.getId(), event.getTitle(), event.getUsername());
        } catch (Exception e) {
            log.error("Failed to parse Protobuf UploadEvent", e);
        }
    }
}
