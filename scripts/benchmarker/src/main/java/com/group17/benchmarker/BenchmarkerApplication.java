package com.group17.benchmarker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group17.benchmarker.config.RabbitConfig;
import com.group17.benchmarker.dto.UploadEventDto;
import com.group17.benchmarker.proto.UploadEvent;
import com.group17.benchmarker.service.BenchmarkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.messaging.handler.annotation.Payload;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@EnableRabbit
@Slf4j
public class BenchmarkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchmarkerApplication.class, args);
    }

    private final ObjectMapper objectMapper;
    private final BenchmarkService benchmarkService;

    public BenchmarkerApplication(ObjectMapper objectMapper,
            com.group17.benchmarker.service.BenchmarkService benchmarkService) {
        this.objectMapper = objectMapper;
        this.benchmarkService = benchmarkService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_JSON)
    public void receiveJsonMessage(@Payload byte[] message) {
        try {
            long start = System.nanoTime();
            UploadEventDto event = objectMapper.readValue(message, UploadEventDto.class);
            long end = System.nanoTime();

            double durationUs = (end - start) / 1000.0;

            benchmarkService.addResult(com.group17.benchmarker.model.BenchmarkResult.builder()
                    .type("JSON")
                    .id(event.getId().toString())
                    .deserializationTimeUs(durationUs)
                    .payloadSize(message.length)
                    .timestamp(System.currentTimeMillis())
                    .build());

            log.info("========================================");
            log.info("--- JSON Benchmark ---");
            log.info("Received JSON UploadEvent ID: {}", event.getId());
            log.info("Title: {}", event.getTitle());
            log.info("Username: {}", event.getUsername());
            log.info("Deserialization time: {} µs", String.format("%.2f", durationUs));
            log.info("Payload size: {} bytes", message.length);
            log.info("========================================");
        } catch (Exception e) {
            log.error("Failed to process JSON message", e);
        }
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_PROTO)
    public void receiveProtoMessage(@Payload byte[] message) {
        try {
            long start = System.nanoTime();
            UploadEvent event = UploadEvent.parseFrom(message);
            long end = System.nanoTime();

            double durationUs = (end - start) / 1000.0;

            benchmarkService.addResult(com.group17.benchmarker.model.BenchmarkResult.builder()
                    .type("PROTO")
                    .id(String.valueOf(event.getId()))
                    .deserializationTimeUs(durationUs)
                    .payloadSize(message.length)
                    .timestamp(System.currentTimeMillis())
                    .build());

            log.info("========================================");
            log.info("--- Protobuf Benchmark ---");
            log.info("Received Protobuf UploadEvent ID: {}", event.getId());
            log.info("Title: {}", event.getTitle());
            log.info("Username: {}", event.getUsername());
            log.info("Deserialization time: {} µs", String.format("%.2f", durationUs));
            log.info("Payload size: {} bytes", message.length);
            log.info("========================================");
        } catch (Exception e) {
            log.error("Failed to process Protobuf message", e);
        }
    }
}
