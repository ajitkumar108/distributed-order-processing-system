package com.example.payment_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.enums.PaymentStatus;
import com.example.payment_service.events.OrderSagaEvent;
import com.example.payment_service.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Business logic of Payment Service. It talks to the database only; Kafka is handled by the consumer/publisher.
 * (In real life this is where you would call Stripe/Razorpay; here we SIMULATE a payment provider.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * Probability (0.0 - 1.0) that a payment is declined. Default 0.2 = 20%.
     * Set PAYMENT_FAILURE_RATE=0 to force success, =1 to force failure when testing the saga.
     * (Not "final" on purpose: Spring injects it after construction.)
     */
    @Value("${payment.failure-rate:0.2}")
    private double failureRate;

    /**
     * Charges the customer. IDEMPOTENT: calling it twice for the same saga returns the first result.
     */
    @Transactional
    public Payment processPayment(OrderSagaEvent event) {

        // 1) Already processed this saga? Return the stored result instead of charging again.
        Optional<Payment> existing = paymentRepository.findBySagaId(event.getSagaId());
        if (existing.isPresent()) {
            log.info("Payment already exists for saga={} (duplicate request) -> status={}",
                    event.getSagaId(), existing.get().getStatus());
            return existing.get();
        }

        // 2) Read the amount from the event payload (numbers in JSON arrive as Integer/Double, so go via String).
        BigDecimal amount = new BigDecimal(String.valueOf(event.getPayload().get("amount")));

        // 3) Simulate the payment provider: randomly decline some payments.
        boolean declined = ThreadLocalRandom.current().nextDouble() < failureRate;

        LocalDateTime now = LocalDateTime.now();
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .sagaId(event.getSagaId())
                .amount(amount)
                .transactionId(UUID.randomUUID().toString())
                .status(declined ? PaymentStatus.FAILED : PaymentStatus.SUCCESS)
                .createdAt(now)
                .updatedAt(now)
                .build();

        log.info("Payment {} for saga={} amount={}", payment.getStatus(), event.getSagaId(), amount);
        return paymentRepository.save(payment);
    }

    /**
     * COMPENSATION: give the money back. Only a SUCCESS payment can be refunded.
     * IDEMPOTENT: refunding an already-REFUNDED payment changes nothing.
     *
     * @return the payment (so the caller can check whether it is REFUNDED), or empty if none exists
     */
    @Transactional
    public Optional<Payment> refundPayment(OrderSagaEvent event) {
        return paymentRepository.findBySagaId(event.getSagaId()).map(payment -> {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setUpdatedAt(LocalDateTime.now());
                log.info("REFUNDED payment for saga={}", event.getSagaId());
            }
            // JPA saves the change automatically at the end of the transaction (the entity is "managed").
            return payment;
        });
    }
}
