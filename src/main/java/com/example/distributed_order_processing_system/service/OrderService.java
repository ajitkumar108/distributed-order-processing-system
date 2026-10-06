package com.example.distributed_order_processing_system.service;


import com.example.distributed_order_processing_system.dto.OrderRequest;
import com.example.distributed_order_processing_system.entity.Order;
import com.example.distributed_order_processing_system.enums.OrderStatus;
import com.example.distributed_order_processing_system.repository.OrderRepository;
import com.example.distributed_order_processing_system.repository.SagaStateRepository;
import com.example.distributed_order_processing_system.saga.SagaState;
import com.example.distributed_order_processing_system.saga.SagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;

    public Order createOrder(OrderRequest request) {

        Order order = Order.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .amount(request.getAmount())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        SagaState sagaState = SagaState.builder()
                .sagaId(UUID.randomUUID())
                .orderId(savedOrder.getId())
                .status(SagaStatus.STARTED)
                .lastEvent("ORDER_CREATED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sagaStateRepository.save(sagaState);

        return savedOrder;
    }
}
