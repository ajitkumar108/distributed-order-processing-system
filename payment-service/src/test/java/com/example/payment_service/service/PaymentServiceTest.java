package com.example.payment_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.enums.PaymentStatus;
import com.example.payment_service.events.EventTypes;
import com.example.payment_service.events.OrderSagaEvent;
import com.example.payment_service.repository.PaymentRepository;

/** Unit tests for the payment business rules (database is a mock, no Kafka, no Spring). */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository repository;
    @InjectMocks PaymentService service;

    private OrderSagaEvent chargeEvent() {
        return OrderSagaEvent.of("saga-1", "order-1", EventTypes.PAYMENT_REQUESTED,
                Map.of("amount", new BigDecimal("100.00")));
    }

    @Test
    void charge_succeeds_whenFailureRateIsZero() {
        ReflectionTestUtils.setField(service, "failureRate", 0.0);   // never decline
        when(repository.findBySagaId("saga-1")).thenReturn(Optional.empty());
        when(repository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.processPayment(chargeEvent());

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
    }

    @Test
    void charge_fails_whenFailureRateIsOne() {
        ReflectionTestUtils.setField(service, "failureRate", 1.0);   // always decline
        when(repository.findBySagaId("saga-1")).thenReturn(Optional.empty());
        when(repository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.processPayment(chargeEvent());

        assertEquals(PaymentStatus.FAILED, result.getStatus());
    }

    @Test
    void charge_isIdempotent_duplicateRequestDoesNotChargeAgain() {
        Payment already = Payment.builder().sagaId("saga-1").status(PaymentStatus.SUCCESS).build();
        when(repository.findBySagaId("saga-1")).thenReturn(Optional.of(already));

        Payment result = service.processPayment(chargeEvent());

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        verify(repository, never()).save(any());   // nothing new was stored
    }

    @Test
    void refund_marksSuccessfulPaymentAsRefunded() {
        Payment paid = Payment.builder().sagaId("saga-1").status(PaymentStatus.SUCCESS).build();
        when(repository.findBySagaId("saga-1")).thenReturn(Optional.of(paid));

        Optional<Payment> result = service.refundPayment(
                OrderSagaEvent.of("saga-1", "order-1", EventTypes.PAYMENT_REFUND_REQUESTED, Map.of()));

        assertEquals(PaymentStatus.REFUNDED, result.orElseThrow().getStatus());
    }

    @Test
    void refund_doesNotRefundAFailedPayment() {
        Payment declined = Payment.builder().sagaId("saga-1").status(PaymentStatus.FAILED).build();
        when(repository.findBySagaId("saga-1")).thenReturn(Optional.of(declined));

        Optional<Payment> result = service.refundPayment(
                OrderSagaEvent.of("saga-1", "order-1", EventTypes.PAYMENT_REFUND_REQUESTED, Map.of()));

        assertEquals(PaymentStatus.FAILED, result.orElseThrow().getStatus());   // unchanged
    }
}
