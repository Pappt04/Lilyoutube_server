package com.group17.lilyoutube_server.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "upload-exchange";
    public static final String QUEUE_JSON = "upload-queue-json";
    public static final String QUEUE_PROTO = "upload-queue-proto";
    public static final String ROUTING_KEY_JSON = "upload.json";
    public static final String ROUTING_KEY_PROTO = "upload.proto";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue jsonQueue() {
        return new Queue(QUEUE_JSON);
    }

    @Bean
    public Queue protoQueue() {
        return new Queue(QUEUE_PROTO);
    }

    @Bean
    public Binding jsonBinding(Queue jsonQueue, TopicExchange exchange) {
        return BindingBuilder.bind(jsonQueue).to(exchange).with(ROUTING_KEY_JSON);
    }

    @Bean
    public Binding protoBinding(Queue protoQueue, TopicExchange exchange) {
        return BindingBuilder.bind(protoQueue).to(exchange).with(ROUTING_KEY_PROTO);
    }
}
