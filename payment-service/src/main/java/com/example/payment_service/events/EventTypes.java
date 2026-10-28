package com.example.payment_service.events;

/** Event type names. Must match the strings used by Order Service exactly. */
public final class EventTypes {

    private EventTypes() { }

    // Requests we receive
    public static final String PAYMENT_REQUESTED        = "PAYMENT_REQUESTED";
    public static final String PAYMENT_REFUND_REQUESTED = "PAYMENT_REFUND_REQUESTED";

    // Results we send back
    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED    = "PAYMENT_FAILED";
    public static final String PAYMENT_REFUNDED  = "PAYMENT_REFUNDED";
}
