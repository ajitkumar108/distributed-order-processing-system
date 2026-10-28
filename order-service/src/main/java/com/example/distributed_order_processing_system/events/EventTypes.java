package com.example.distributed_order_processing_system.events;

/**
 * Values of OrderSagaEvent.eventType. A "request" asks a service to do something;
 * a "result" tells the orchestrator what happened.
 * (Must be compile-time constants so they can be used in switch/case.)
 */
public final class EventTypes {

    private EventTypes() { }

    // ----- Requests sent BY Order Service -----
    public static final String PAYMENT_REQUESTED           = "PAYMENT_REQUESTED";
    public static final String PAYMENT_REFUND_REQUESTED    = "PAYMENT_REFUND_REQUESTED";   // compensation
    public static final String INVENTORY_RESERVE_REQUESTED = "INVENTORY_RESERVE_REQUESTED";

    // ----- Results received FROM Payment Service -----
    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED    = "PAYMENT_FAILED";
    public static final String PAYMENT_REFUNDED  = "PAYMENT_REFUNDED";

    // ----- Results received FROM Inventory Service -----
    public static final String INVENTORY_RESERVED = "INVENTORY_RESERVED";
    public static final String INVENTORY_FAILED   = "INVENTORY_FAILED";
}
