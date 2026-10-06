package com.example.distributed_order_processing_system.saga;


public enum SagaStatus {

    STARTED,

    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,

    INVENTORY_PENDING,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,

    COMPLETED,

    COMPENSATING,
    COMPENSATED,

    FAILED
}