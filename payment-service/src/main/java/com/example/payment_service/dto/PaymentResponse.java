package com.example.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.payment_service.enums.PaymentStatus;

/** What the read-only REST endpoint returns (we don't expose the JPA entity directly). */
public record PaymentResponse(
        String orderId,
        String sagaId,
        BigDecimal amount,
        String transactionId,
        PaymentStatus status,
        LocalDateTime createdAt
) { }
