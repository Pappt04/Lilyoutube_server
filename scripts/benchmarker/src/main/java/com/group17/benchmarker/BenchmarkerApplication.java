package com.group17.benchmarker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group17.benchmarker.config.RabbitConfig;
import com.group17.benchmarker.dto.UploadEvent;
import com.group17.benchmarker.proto.UploadEventProto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Set;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@Slf4j
public class BenchmarkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BenchmarkerApplication.class, args);
	}

	@Bean
	public CommandLineRunner runBenchmark(ObjectMapper objectMapper, RabbitTemplate rabbitTemplate) {
		return args -> {
			int iterations = 1000;
			int warmup = 100;

			UploadEvent jsonEvent = createSampleJsonEvent();
			com.group17.benchmarker.proto.UploadEvent protoEvent = createSampleProtoEvent();

			log.info("Starting Benchmark with {} iterations...", iterations);

			// JSON Serialization
			long totalJsonSerTime = 0;
			byte[] jsonBytes = null;
			for (int i = 0; i < iterations + warmup; i++) {
				long start = System.nanoTime();
				jsonBytes = objectMapper.writeValueAsBytes(jsonEvent);
				long end = System.nanoTime();
				if (i >= warmup)
					totalJsonSerTime += (end - start);
			}
			double avgJsonSerTime = totalJsonSerTime / (double) iterations / 1000.0;
			int jsonSize = jsonBytes.length;

			// Protobuf Serialization
			long totalProtoSerTime = 0;
			byte[] protoBytes = null;
			for (int i = 0; i < iterations + warmup; i++) {
				long start = System.nanoTime();
				protoBytes = protoEvent.toByteArray();
				long end = System.nanoTime();
				if (i >= warmup)
					totalProtoSerTime += (end - start);
			}
			double avgProtoSerTime = totalProtoSerTime / (double) iterations / 1000.0;
			int protoSize = protoBytes.length;

			// JSON Deserialization
			long totalJsonDesTime = 0;
			for (int i = 0; i < iterations + warmup; i++) {
				long start = System.nanoTime();
				objectMapper.readValue(jsonBytes, UploadEvent.class);
				long end = System.nanoTime();
				if (i >= warmup)
					totalJsonDesTime += (end - start);
			}
			double avgJsonDesTime = totalJsonDesTime / (double) iterations / 1000.0;

			// Protobuf Deserialization
			long totalProtoDesTime = 0;
			for (int i = 0; i < iterations + warmup; i++) {
				long start = System.nanoTime();
				com.group17.benchmarker.proto.UploadEvent.parseFrom(protoBytes);
				long end = System.nanoTime();
				if (i >= warmup)
					totalProtoDesTime += (end - start);
			}
			double avgProtoDesTime = totalProtoDesTime / (double) iterations / 1000.0;

			log.info("--- Benchmark Results ---");
			log.info("JSON: Avg Ser: {} µs, Avg Des: {} µs, Size: {} bytes", avgJsonSerTime, avgJsonDesTime, jsonSize);
			log.info("Proto: Avg Ser: {} µs, Avg Des: {} µs, Size: {} bytes", avgProtoSerTime, avgProtoDesTime,
					protoSize);
			log.info("Ratio (JSON/Proto): Ser: {}, Des: {}, Size: {}",
					avgJsonSerTime / avgProtoSerTime, avgJsonDesTime / avgProtoDesTime, (double) jsonSize / protoSize);

			log.info("Sending messages to RabbitMQ...");
			rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY_JSON, jsonBytes);
			log.info("Sent JSON message to RabbitMQ");
			rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY_PROTO, protoBytes);
			log.info("Sent Proto message to RabbitMQ");
		};
	}

	private UploadEvent createSampleJsonEvent() {
		return UploadEvent.builder()
				.id(1L)
				.title("Test Video")
				.description("This is a test video description with some length to it.")
				.thumbnailPath("/media/thumbnails/test.jpg")
				.videoPath("/media/videos/test.mp4")
				.location("Novi Sad, Serbia")
				.tags(Set.of("tag1", "tag2", "tag3", "java", "benchmark"))
				.createdAt("2026-01-26T17:24:00")
				.likesCount(100)
				.commentsCount(50)
				.viewsCount(5000)
				.userId(10L)
				.username("anonymous")
				.email("anonymous@example.com")
				.firstName("Anonymous")
				.lastName("Somebody")
				.address("Street 1, Novi Sad")
				.enabled(true)
				.activationToken("token123")
				.build();
	}

	private com.group17.benchmarker.proto.UploadEvent createSampleProtoEvent() {
		return com.group17.benchmarker.proto.UploadEvent.newBuilder()
				.setId(1L)
				.setTitle("Test Video")
				.setDescription("This is a test video description with some length to it.")
				.setThumbnailPath("/media/thumbnails/test.jpg")
				.setVideoPath("/media/videos/test.mp4")
				.setLocation("Novi Sad, Serbia")
				.addAllTags(Set.of("tag1", "tag2", "tag3", "java", "benchmark"))
				.setCreatedAt("2026-01-26T17:24:00")
				.setLikesCount(100)
				.setCommentsCount(50)
				.setViewsCount(5000)
				.setUserId(10L)
				.setUsername("anonymous")
				.setEmail("anonymous@example.com")
				.setFirstName("Anonymous")
				.setLastName("Somebody")
				.setAddress("Street 1, Novi Sad")
				.setEnabled(true)
				.setActivationToken("token123")
				.build();
	}
}
