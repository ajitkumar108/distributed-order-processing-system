package com.example.distributed_order_processing_system.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.distributed_order_processing_system.entity.Order;

/** Spring Data generates the SQL for us from method names. No implementation class needed. */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** SELECT * FROM orders WHERE idempotency_key = ? */
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
