package com.example.distributed_order_processing_system.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.distributed_order_processing_system.events.OrderSagaEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Single place where Order Service SENDS events to Kafka.
 *
 * Key design point (avoids a classic bug): if we published to Kafka INSIDE a database transaction and the
 * transaction later rolled back, Payment would process an order that doesn't exist. So when a transaction is
 * running we wait until it has COMMITTED and only then send ("afterCommit").
 *
 * Remaining gap (honest limitation): if the app crashes between "DB commit" and "Kafka send", the message is lost.
 * The proper cure is the Transactional Outbox pattern; here SagaRecoveryJob re-sends stuck requests instead.
 */
@Component
@RequiredArgsConstructor   // Lombok creates a constructor for the final fields => Spring injects them
@Slf4j                     // Lombok gives us a ready-made "log" variable
public class SagaEventPublisher {

    /** Sends text messages. Spring Boot auto-creates it from the spring.kafka.* properties. */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /** Spring Boot's pre-configured JSON converter (it already understands LocalDateTime). */
    private final ObjectMapper objectMapper;

    /** Publish an event to a topic (immediately, or after the current DB transaction commits). */
    public void publish(String topic, OrderSagaEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // We are inside @Transactional code: delay the send until the commit succeeded.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(topic, event);
                }
            });
        } else {
            // No transaction running: just send now.
            send(topic, event);
        }
    }

    private void send(String topic, OrderSagaEvent event) {
        try {
            // Java object -> JSON text
            String json = objectMapper.writeValueAsString(event);

            // key = orderId: Kafka puts same-key messages in the same partition => per-order ordering.
            kafkaTemplate.send(topic, event.getOrderId(), json)
                    .whenComplete((result, error) -> {   // runs when Kafka answers (async)
                        if (error != null) {
                            log.error("KAFKA SEND FAILED topic={} type={} saga={}", topic, event.getEventType(), event.getSagaId(), error);
                        } else {
                            log.info("KAFKA SENT topic={} type={} saga={} partition={} offset={}",
                                    topic, event.getEventType(), event.getSagaId(),
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event " + event.getEventType(), e);
        }
    }
}
