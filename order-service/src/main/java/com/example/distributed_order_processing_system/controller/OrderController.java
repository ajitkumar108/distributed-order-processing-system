package com.example.distributed_order_processing_system.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.distributed_order_processing_system.dto.OrderRequest;
import com.example.distributed_order_processing_system.dto.OrderResponse;
import com.example.distributed_order_processing_system.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API of Order Service (open Swagger UI at http://localhost:8080/swagger-ui.html).
 * Through the gateway: http://localhost:9090/orders
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /orders  -> creates an order and starts the saga.
     * The response returns immediately with orderStatus=PENDING; the saga finishes asynchronously.
     * Poll GET /orders/{id} to see the final status.
     *
     * @Valid triggers the validation annotations of OrderRequest (400 if invalid).
     * Idempotency-Key header is optional: send the same value when retrying to avoid duplicate orders.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, idempotencyKey));
    }

    /** GET /orders/{id} -> order + saga status (PENDING -> CONFIRMED / FAILED / COMPENSATED). */
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }
}
