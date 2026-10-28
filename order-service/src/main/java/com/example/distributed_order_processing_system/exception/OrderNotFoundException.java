package com.example.distributed_order_processing_system.exception;

import java.util.UUID;

/** Thrown when GET /orders/{id} asks for an order that does not exist. Mapped to HTTP 404 below. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Order not found: " + id);
    }
}
