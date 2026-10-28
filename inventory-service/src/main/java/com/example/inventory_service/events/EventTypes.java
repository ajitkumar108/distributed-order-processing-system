package com.example.inventory_service.events;

/** Event type names (must match Order Service's strings exactly). */
public final class EventTypes {

    private EventTypes() { }

    public static final String INVENTORY_RESERVE_REQUESTED = "INVENTORY_RESERVE_REQUESTED";  // request we receive
    public static final String INVENTORY_RESERVED          = "INVENTORY_RESERVED";           // success answer
    public static final String INVENTORY_FAILED            = "INVENTORY_FAILED";             // failure answer
}
