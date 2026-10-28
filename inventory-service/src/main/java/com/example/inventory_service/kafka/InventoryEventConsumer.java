package com.example.inventory_service.kafka;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.inventory_service.entity.Reservation;
import com.example.inventory_service.enums.ReservationStatus;
import com.example.inventory_service.events.EventTypes;
import com.example.inventory_service.events.OrderSagaEvent;
import com.example.inventory_service.events.Topics;
import com.example.inventory_service.service.InventoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Receives reservation requests from Order Service and answers with the outcome. */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryEventPublisher publisher;
    private final ObjectMapper objectMapper;

    // Group id comes from application.properties (spring.kafka.consumer.group-id).
    @KafkaListener(topics = Topics.INVENTORY_REQUESTS)
    public void onMessage(String message) {
        OrderSagaEvent request = parse(message);
        log.info("RECEIVED inventory-requests type={} saga={}", request.getEventType(), request.getSagaId());

        if (!EventTypes.INVENTORY_RESERVE_REQUESTED.equals(request.getEventType())) {
            log.warn("Ignoring unknown event type {}", request.getEventType());
            return;
        }

        Reservation reservation = inventoryService.reserve(request);   // DB work (idempotent)

        boolean ok = reservation.getStatus() == ReservationStatus.RESERVED;
        String resultType = ok ? EventTypes.INVENTORY_RESERVED : EventTypes.INVENTORY_FAILED;

        // Map.of() cannot hold null values, so use an empty string when there is no failure reason.
        publisher.publish(OrderSagaEvent.of(
                request.getSagaId(), request.getOrderId(), resultType,
                Map.of("reason", reservation.getReason() == null ? "" : reservation.getReason())));
    }

    private OrderSagaEvent parse(String message) {
        try {
            return objectMapper.readValue(message, OrderSagaEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid event JSON: " + message, e);
        }
    }
}
