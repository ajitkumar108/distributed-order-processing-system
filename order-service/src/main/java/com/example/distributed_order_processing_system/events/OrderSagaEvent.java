package com.example.distributed_order_processing_system.events;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The ONE message format used on every Kafka topic (sent as JSON text).
 * Each service has its own copy of this class (microservices don't share code at runtime);
 * only the JSON field names must match.
 */
@Data                // Lombok: getters, setters, equals, hashCode, toString
@NoArgsConstructor   // Needed by Jackson (JSON library) to create the object when reading JSON
@AllArgsConstructor
@Builder             // Lets us write OrderSagaEvent.builder().sagaId(...).build()
public class OrderSagaEvent {

    /** Correlation id of the whole saga (one saga = one order's journey). Same value in every event. */
    private String sagaId;

    /** The order this event is about. Also used as the Kafka message key so one order's events stay in order. */
    private String orderId;

    /** What happened / what is requested. See EventTypes. */
    private String eventType;

    /** When the event was created. */
    private LocalDateTime timestamp;

    /** Free-form extra data (amount, productId, quantity, reason, ...). */
    private Map<String, Object> payload;

    /** Convenience factory so callers don't repeat the builder boilerplate. */
    public static OrderSagaEvent of(String sagaId, String orderId, String eventType, Map<String, Object> payload) {
        return OrderSagaEvent.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();
    }
}
