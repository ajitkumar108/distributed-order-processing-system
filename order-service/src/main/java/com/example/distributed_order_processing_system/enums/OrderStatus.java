package com.example.distributed_order_processing_system.enums;

/**
 * Business status of an order (what the CUSTOMER cares about).
 * Compare with SagaStatus, which is the TECHNICAL progress of the distributed transaction.
 */
public enum OrderStatus {
    STARTED,
    PENDING,       // created, saga in progress
    CONFIRMED,     // payment taken AND stock reserved
    FAILED,        // payment failed (nothing to undo)
    COMPENSATING,  // payment was taken but stock failed -> refund in progress
    COMPENSATED    // refund done, order cancelled
}
