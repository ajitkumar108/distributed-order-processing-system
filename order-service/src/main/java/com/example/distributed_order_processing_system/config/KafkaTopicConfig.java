package com.example.distributed_order_processing_system.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.example.distributed_order_processing_system.events.Topics;

/**
 * Creates the Kafka topics at startup if they do not exist yet (Spring's KafkaAdmin does the work).
 * Order Service uses all four topics, so it declares all four.
 *
 * partitions(3): up to 3 consumers of the same group can work in parallel.
 * replicas(1):   we only have ONE broker in this demo (production would use 3).
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

    @Bean
    public NewTopic inventoryRequestsTopic() {
        return TopicBuilder.name(Topics.INVENTORY_REQUESTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(Topics.INVENTORY_EVENTS).partitions(3).replicas(1).build();
    }
}
