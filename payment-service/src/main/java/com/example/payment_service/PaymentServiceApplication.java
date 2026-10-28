package com.example.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PAYMENT SERVICE.
 *
 * Listens on Kafka for requests from Order Service ("charge this order", "refund this order"),
 * does the work in its OWN database (payment_db) and answers on Kafka.
 * It never calls Order Service or Inventory Service directly.
 *
 * NOTE: @EnableKafka is not needed - Spring Boot turns Kafka support on automatically
 * when spring-kafka is on the classpath.
 *
 * IMPORTANT for @KafkaListener to work: all classes must live in THIS package or below it,
 * because @SpringBootApplication only scans its own package tree.
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
