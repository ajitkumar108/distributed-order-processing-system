package com.example.distributed_order_processing_system.saga;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity 
@Table(name = "saga_state")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class SagaState {
    @Id 
    private UUID sagaId;
    
    private UUID orderId;
    @Enumerated (EnumType.STRING)
    private SagaStatus status;

    private String lastEvent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
