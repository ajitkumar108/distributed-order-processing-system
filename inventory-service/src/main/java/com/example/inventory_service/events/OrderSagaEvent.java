package com.example.inventory_service.events;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Same JSON message shape as in the other services (own copy; only JSON field names must match). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSagaEvent {

    private String sagaId;
    private String orderId;
    private String eventType;
    private LocalDateTime timestamp;
    private Map<String, Object> payload;

    public static OrderSagaEvent of(String sagaId, String orderId, String eventType, Map<String, Object> payload) {
        return OrderSagaEvent.builder()
                .sagaId(sagaId).orderId(orderId).eventType(eventType)
                .timestamp(LocalDateTime.now()).payload(payload).build();
    }
}
