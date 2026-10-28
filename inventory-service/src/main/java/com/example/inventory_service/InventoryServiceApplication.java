package com.example.inventory_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * INVENTORY SERVICE.
 *
 * Owns the product stock (its own database inventory_db). When Order Service asks "reserve 2 units of PROD-1",
 * it decrements the stock if enough exists and answers INVENTORY_RESERVED, otherwise INVENTORY_FAILED
 * (which makes Order Service refund the payment = compensation).
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
