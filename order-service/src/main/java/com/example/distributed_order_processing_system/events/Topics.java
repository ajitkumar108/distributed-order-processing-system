package com.example.distributed_order_processing_system.events;

/**
 * Names of the Kafka topics. Keeping them as constants avoids typos
 * (a typo in a topic name = silent "my listener never fires" bug!).
 *
 * Direction:
 *   payment-requests   Order     -> Payment
 *   payment-events     Payment   -> Order
 *   inventory-requests Order     -> Inventory
 *   inventory-events   Inventory -> Order
 */
public final class Topics {

    private Topics() { } // utility class: prevent "new Topics()"

    public static final String PAYMENT_REQUESTS   = "payment-requests";
    public static final String PAYMENT_EVENTS     = "payment-events";
    public static final String INVENTORY_REQUESTS = "inventory-requests";
    public static final String INVENTORY_EVENTS   = "inventory-events";
}
