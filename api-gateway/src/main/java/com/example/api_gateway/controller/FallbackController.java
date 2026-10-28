package com.example.api_gateway.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FALLBACK endpoint used by the circuit breaker.
 *
 * When Order Service is down/slow and the circuit is OPEN, the gateway "forwards" the request here
 * instead of making the client wait for a timeout. The client gets a fast, friendly 503 JSON answer.
 */
@RestController
public class FallbackController {

    // @RequestMapping without a method = accepts GET, POST, ... (the original request method is preserved).
    @RequestMapping("/fallback/orders")
    public ResponseEntity<Map<String, String>> orderFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Order service is temporarily unavailable",
                        "hint", "Please retry in a few seconds"));
    }
}
