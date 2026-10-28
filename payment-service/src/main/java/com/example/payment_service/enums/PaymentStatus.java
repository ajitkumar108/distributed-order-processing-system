package com.example.payment_service.enums;

/** Life cycle of one payment row. */
public enum PaymentStatus {
    SUCCESS,   // customer was charged
    FAILED,    // charge was declined (nothing was taken)
    REFUNDED   // charged earlier, then money returned (compensation)
}
