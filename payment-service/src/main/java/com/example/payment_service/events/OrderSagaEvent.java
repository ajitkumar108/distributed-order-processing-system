package com.example.payment_service.events;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Same JSON message shape as in Order Service (own copy: services don't share Java code).
 * Only the JSON field names must match; the package name does NOT matter because we send plain JSON text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSagaEvent {

    private String sagaId;                   // identifies the whole saga
    private String orderId;                  // the order concerned
    private String eventType;                // see EventTypes
    private LocalDateTime timestamp;
    private Map<String, Object> payload;     // extra data (amount, productId, quantity, ...)

    public static OrderSagaEvent of(String sagaId, String orderId, String eventType, Map<String, Object> payload) {
        return OrderSagaEvent.builder()
                .sagaId(sagaId).orderId(orderId).eventType(eventType)
                .timestamp(LocalDateTime.now()).payload(payload).build();
    }
}
