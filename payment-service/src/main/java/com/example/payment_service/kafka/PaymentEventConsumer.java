package com.example.payment_service.kafka;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.enums.PaymentStatus;
import com.example.payment_service.events.EventTypes;
import com.example.payment_service.events.OrderSagaEvent;
import com.example.payment_service.events.Topics;
import com.example.payment_service.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Receives requests from Order Service on "payment-requests" and answers on "payment-events".
 *
 * NO groupId in the annotation: the group comes from spring.kafka.consumer.group-id in application.properties
 * (a groupId in the annotation silently overrides the property and confuses debugging).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final PaymentEventPublisher publisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.PAYMENT_REQUESTS)
    public void onMessage(String message) {
        OrderSagaEvent event = parse(message);
        log.info("RECEIVED payment-requests type={} saga={}", event.getEventType(), event.getSagaId());

        switch (event.getEventType()) {
            case EventTypes.PAYMENT_REQUESTED        -> handleCharge(event);
            case EventTypes.PAYMENT_REFUND_REQUESTED -> handleRefund(event);
            default -> log.warn("Ignoring unknown event type {}", event.getEventType());
        }
    }

    /** Charge the customer, then tell Order Service the outcome. */
    private void handleCharge(OrderSagaEvent request) {
        Payment payment = paymentService.processPayment(request);   // DB work, idempotent

        // SUCCESS or REFUNDED both mean "the charge itself succeeded"; only FAILED is a failure.
        String resultType = payment.getStatus() == PaymentStatus.FAILED
                ? EventTypes.PAYMENT_FAILED
                : EventTypes.PAYMENT_COMPLETED;

        publisher.publish(OrderSagaEvent.of(
                request.getSagaId(), request.getOrderId(), resultType,
                Map.of("transactionId", payment.getTransactionId(),
                       "status", payment.getStatus().name())));
    }

    /** Compensation: refund, then confirm to Order Service. */
    private void handleRefund(OrderSagaEvent request) {
        paymentService.refundPayment(request).ifPresentOrElse(
                payment -> {
                    // Confirm only if the money is really back.
                    if (payment.getStatus() == PaymentStatus.REFUNDED) {
                        publisher.publish(OrderSagaEvent.of(
                                request.getSagaId(), request.getOrderId(), EventTypes.PAYMENT_REFUNDED,
                                Map.of("transactionId", payment.getTransactionId())));
                    } else {
                        log.warn("Cannot refund saga={} because payment status is {}",
                                request.getSagaId(), payment.getStatus());
                    }
                },
                () -> log.warn("Refund requested but no payment exists for saga={}", request.getSagaId()));
    }

    private OrderSagaEvent parse(String message) {
        try {
            return objectMapper.readValue(message, OrderSagaEvent.class);   // JSON text -> object
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid event JSON: " + message, e);
        }
    }
}
