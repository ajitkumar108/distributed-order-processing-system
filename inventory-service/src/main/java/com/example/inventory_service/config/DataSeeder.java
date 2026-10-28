package com.example.inventory_service.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.inventory_service.entity.Product;
import com.example.inventory_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Inserts demo products at startup (only when the table is empty), so you can test immediately:
 *   PROD-1 : 100 in stock -> normal successful orders
 *   PROD-2 : 5 in stock   -> order quantity 10 forces INVENTORY_FAILED -> refund (compensation demo)
 *   PROD-3 : 0 in stock   -> always out of stock
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            productRepository.save(new Product("PROD-1", "Laptop", 100));
            productRepository.save(new Product("PROD-2", "Phone", 5));
            productRepository.save(new Product("PROD-3", "Tablet", 0));
            log.info("Seeded demo products PROD-1, PROD-2, PROD-3");
        }
    }
}
