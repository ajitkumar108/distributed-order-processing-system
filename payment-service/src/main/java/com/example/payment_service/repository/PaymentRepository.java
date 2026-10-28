package com.example.payment_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.payment_service.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** Used for idempotency and for refunds: "do we already have a payment for this saga?" */
    Optional<Payment> findBySagaId(String sagaId);

    /** Used by the read-only REST endpoint. */
    Optional<Payment> findByOrderId(String orderId);
}
