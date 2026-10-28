package com.example.distributed_order_processing_system.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.distributed_order_processing_system.saga.SagaState;
import com.example.distributed_order_processing_system.saga.SagaStatus;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {

    /** Find the saga of an order (used by GET /orders/{id}). */
    Optional<SagaState> findByOrderId(UUID orderId);

    /**
     * Used by the recovery job:
     * SELECT * FROM saga_state WHERE status NOT IN (...) AND updated_at < ?
     * = "sagas that are still running but have not changed for a while" = probably stuck.
     */
    List<SagaState> findByStatusNotInAndUpdatedAtBefore(Collection<SagaStatus> statuses, LocalDateTime cutoff);
}
