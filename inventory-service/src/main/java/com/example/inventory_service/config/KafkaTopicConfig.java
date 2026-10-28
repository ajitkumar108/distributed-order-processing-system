package com.example.inventory_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.example.inventory_service.events.Topics;

/** Declares the two topics this service uses (created only if missing). */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic inventoryRequestsTopic() {
        return TopicBuilder.name(Topics.INVENTORY_REQUESTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(Topics.INVENTORY_EVENTS).partitions(3).replicas(1).build();
    }
}
