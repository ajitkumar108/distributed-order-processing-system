package com.example.inventory_service.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.inventory_service.events.OrderSagaEvent;
import com.example.inventory_service.events.Topics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sends INVENTORY_RESERVED / INVENTORY_FAILED to the "inventory-events" topic. */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(OrderSagaEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(Topics.INVENTORY_EVENTS, event.getOrderId(), json)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("KAFKA SEND FAILED type={} saga={}", event.getEventType(), event.getSagaId(), error);
                        } else {
                            log.info("KAFKA SENT inventory-events type={} saga={} offset={}",
                                    event.getEventType(), event.getSagaId(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event", e);
        }
    }
}
