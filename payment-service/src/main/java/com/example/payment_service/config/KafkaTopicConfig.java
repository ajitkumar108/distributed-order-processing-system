package com.example.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.example.payment_service.events.Topics;

/**
 * Declares the two topics this service uses. If they already exist (created by Order Service) nothing happens;
 * declaring them here means Payment Service also works if it happens to start first.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentRequestsTopic() {
        return TopicBuilder.name(Topics.PAYMENT_REQUESTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(Topics.PAYMENT_EVENTS).partitions(3).replicas(1).build();
    }
}
