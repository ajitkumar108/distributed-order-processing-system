package com.example.distributed_order_processing_system.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.distributed_order_processing_system.events.EventTypes;
import com.example.distributed_order_processing_system.events.OrderSagaEvent;
import com.example.distributed_order_processing_system.events.Topics;
import com.example.distributed_order_processing_system.saga.SagaOrchestrator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Order Service LISTENS here for the answers of Payment and Inventory.
 * It only translates "Kafka message" -> "method call on the orchestrator". All decisions live in SagaOrchestrator.
 *
 * Both listeners use the same group id (spring.kafka.consumer.group-id in application.properties),
 * so if you run several Order Service instances, each message is handled by only ONE of them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventConsumer {

    private final SagaOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    /** Results coming from Payment Service. */
    @KafkaListener(topics = Topics.PAYMENT_EVENTS)
    public void onPaymentEvent(String message) {
        OrderSagaEvent event = parse(message);
        log.info("RECEIVED payment-events type={} saga={}", event.getEventType(), event.getSagaId());

        switch (event.getEventType()) {
            case EventTypes.PAYMENT_COMPLETED -> orchestrator.onPaymentCompleted(event);
            case EventTypes.PAYMENT_FAILED    -> orchestrator.onPaymentFailed(event);
            case EventTypes.PAYMENT_REFUNDED  -> orchestrator.onPaymentRefunded(event);
            default -> log.warn("Ignoring unknown payment event type {}", event.getEventType());
        }
    }

    /** Results coming from Inventory Service. */
    @KafkaListener(topics = Topics.INVENTORY_EVENTS)
    public void onInventoryEvent(String message) {
        OrderSagaEvent event = parse(message);
        log.info("RECEIVED inventory-events type={} saga={}", event.getEventType(), event.getSagaId());

        switch (event.getEventType()) {
            case EventTypes.INVENTORY_RESERVED -> orchestrator.onInventoryReserved(event);
            case EventTypes.INVENTORY_FAILED   -> orchestrator.onInventoryFailed(event);
            default -> log.warn("Ignoring unknown inventory event type {}", event.getEventType());
        }
    }

    /**
     * JSON text -> OrderSagaEvent. If the text is not valid JSON we throw; Spring Kafka then retries a few
     * times and finally logs and skips the message (so one bad message can't block the topic forever).
     */
    private OrderSagaEvent parse(String message) {
        try {
            return objectMapper.readValue(message, OrderSagaEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid event JSON: " + message, e);
        }
    }
}
