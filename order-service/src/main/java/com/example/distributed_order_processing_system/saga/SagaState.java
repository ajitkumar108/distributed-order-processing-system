package com.example.distributed_order_processing_system.saga;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per saga, stored in MySQL (table saga_state).
 *
 * WHY persist it? If Order Service crashes in the middle, the row tells us exactly where the saga stopped,
 * so SagaRecoveryJob can resume it after restart. Without this table, in-flight orders would be lost.
 */
@Entity                       // JPA: this class is a database table
@Table(name = "saga_state")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SagaState {

    /** Primary key. We generate it ourselves (UUID) so we can put it into events before saving. */
    @Id
    private UUID sagaId;

    /** The order this saga belongs to. */
    @Column(nullable = false)
    private UUID orderId;

    /** Current step of the state machine. Stored as text (readable in SQL) instead of a number. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SagaStatus status;

    /** Name of the last event that moved the saga (useful for debugging/audit). */
    private String lastEvent;

    /** How many times the recovery job re-sent the current request. Reset when the saga makes progress. */
    private int retryCount;

    private LocalDateTime createdAt;

    /** Updated on every change. The recovery job treats "old updatedAt + not terminal" as "stuck". */
    private LocalDateTime updatedAt;
}
