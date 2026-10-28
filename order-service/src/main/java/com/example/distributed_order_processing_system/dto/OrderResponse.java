package com.example.distributed_order_processing_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.distributed_order_processing_system.enums.OrderStatus;
import com.example.distributed_order_processing_system.saga.SagaStatus;

/** What the API returns: order data + the saga's progress, so clients can poll GET /orders/{id}. */
public record OrderResponse(
        UUID orderId,
        UUID sagaId,
        String productId,
        int quantity,
        BigDecimal amount,
        OrderStatus orderStatus,
        SagaStatus sagaStatus,
        LocalDateTime createdAt
) { }
