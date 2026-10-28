package com.example.inventory_service.enums;

/** Outcome of one reservation attempt. */
public enum ReservationStatus {
    RESERVED,  // stock was decremented
    FAILED     // not enough stock / unknown product (stock untouched)
}
