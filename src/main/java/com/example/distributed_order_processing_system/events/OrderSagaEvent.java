package com.example.distributed_order_processing_system.events;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.*;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class OrderSagaEvent {
    private String sagaId;
    private String orderId;
    private String eventType;
    private LocalDateTime timestamp;
    private Map<String, Object>payload;
}
