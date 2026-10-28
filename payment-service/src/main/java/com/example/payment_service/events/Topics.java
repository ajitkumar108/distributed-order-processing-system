package com.example.payment_service.events;

/** Kafka topic names (same constants as Order Service; each service keeps its own copy). */
public final class Topics {

    private Topics() { }

    public static final String PAYMENT_REQUESTS = "payment-requests";  // we CONSUME this
    public static final String PAYMENT_EVENTS   = "payment-events";    // we PRODUCE to this
}
