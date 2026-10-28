package com.example.distributed_order_processing_system.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.distributed_order_processing_system.dto.OrderRequest;
import com.example.distributed_order_processing_system.dto.OrderResponse;
import com.example.distributed_order_processing_system.entity.Order;
import com.example.distributed_order_processing_system.enums.OrderStatus;
import com.example.distributed_order_processing_system.exception.OrderNotFoundException;
import com.example.distributed_order_processing_system.repository.OrderRepository;
import com.example.distributed_order_processing_system.repository.SagaStateRepository;
import com.example.distributed_order_processing_system.saga.SagaOrchestrator;
import com.example.distributed_order_processing_system.saga.SagaState;

import lombok.RequiredArgsConstructor;

/**
 * Application service used by the REST controller: creates orders and reads them back.
 * The saga logic itself is delegated to SagaOrchestrator.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final SagaOrchestrator sagaOrchestrator;

    /**
     * Creates an order (status PENDING) and starts its saga.
     * Order row + saga row are saved in ONE transaction; the Kafka message is sent after the commit.
     *
     * @param idempotencyKey optional header value; a repeated key returns the ORIGINAL order instead of a new one
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String idempotencyKey) {

        // 1) Retry protection: same key seen before? Return the existing order, create nothing.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        // 2) Save the order itself.
        Order order = Order.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .amount(request.amount())
                .status(OrderStatus.PENDING)
                .idempotencyKey((idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        // 3) Start the saga (saves saga row + queues the PAYMENT_REQUESTED Kafka message).
        SagaState saga = sagaOrchestrator.startSaga(order);

        return toResponse(order, saga);
    }

    /** Reads an order together with the saga's current progress. */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    // ---------- mapping helpers (entity -> API response) ----------

    private OrderResponse toResponse(Order order) {
        SagaState saga = sagaStateRepository.findByOrderId(order.getId()).orElse(null);
        return toResponse(order, saga);
    }

    private OrderResponse toResponse(Order order, SagaState saga) {
        return new OrderResponse(
                order.getId(),
                saga == null ? null : saga.getSagaId(),
                order.getProductId(),
                order.getQuantity(),
                order.getAmount(),
                order.getStatus(),
                saga == null ? null : saga.getStatus(),
                order.getCreatedAt());
    }
}
