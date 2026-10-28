package com.example.distributed_order_processing_system.saga;

import java.util.List;

/**
 * The state machine of the saga (technical progress).
 *
 *  PAYMENT_PENDING --payment ok--> INVENTORY_PENDING --stock ok--> COMPLETED
 *        |                               |
 *   payment failed                 stock failed
 *        v                               v
 *      FAILED                      COMPENSATING --refund done--> COMPENSATED
 */
public enum SagaStatus {
    PAYMENT_PENDING,    // waiting for Payment Service's answer
    INVENTORY_PENDING,  // payment succeeded, waiting for Inventory Service's answer
    COMPENSATING,       // undoing the payment (refund requested)
    COMPLETED,          // terminal: success
    FAILED,             // terminal: failed, nothing needed undoing (or compensation gave up)
    COMPENSATED;        // terminal: failed but everything was undone

    /** Statuses after which nothing more will happen. Used by the recovery job and for duplicate detection. */
    public static final List<SagaStatus> TERMINAL = List.of(COMPLETED, FAILED, COMPENSATED);
}
