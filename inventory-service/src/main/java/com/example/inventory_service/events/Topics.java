package com.example.inventory_service.events;

/** Kafka topic names used by Inventory Service. */
public final class Topics {

    private Topics() { }

    public static final String INVENTORY_REQUESTS = "inventory-requests";  // we CONSUME this
    public static final String INVENTORY_EVENTS   = "inventory-events";    // we PRODUCE to this
}
