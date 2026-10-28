package com.example.payment_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.payment_service.dto.PaymentResponse;
import com.example.payment_service.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Read-only API to look at payments (handy for demos: "show me that the payment was REFUNDED").
 * Payments are NOT created by REST; they are created by Kafka requests from Order Service.
 * Swagger UI: http://localhost:8082/swagger-ui.html
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    /** GET /payments/order/{orderId} -> the payment of that order, or 404. */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> byOrder(@PathVariable String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(p -> new PaymentResponse(p.getOrderId(), p.getSagaId(), p.getAmount(),
                        p.getTransactionId(), p.getStatus(), p.getCreatedAt()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Simple check that the service is up: http://localhost:8082/payments/ */
    @GetMapping("/")
    public String home() {
        return "Payment Service is running";
    }
}
