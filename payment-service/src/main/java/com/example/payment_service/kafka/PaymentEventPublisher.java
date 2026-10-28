package com.example.payment_service.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.payment_service.events.OrderSagaEvent;
import com.example.payment_service.events.Topics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sends result events (completed / failed / refunded) to the "payment-events" topic. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(OrderSagaEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);   // object -> JSON text
            // key = orderId keeps all events of one order in the same partition (ordered).
            kafkaTemplate.send(Topics.PAYMENT_EVENTS, event.getOrderId(), json)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error("KAFKA SEND FAILED type={} saga={}", event.getEventType(), event.getSagaId(), error);
                        } else {
                            log.info("KAFKA SENT payment-events type={} saga={} offset={}",
                                    event.getEventType(), event.getSagaId(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event", e);
        }
    }
}
