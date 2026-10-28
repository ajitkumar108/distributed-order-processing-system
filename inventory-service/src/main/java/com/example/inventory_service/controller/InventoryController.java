package com.example.inventory_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.inventory_service.entity.Product;
import com.example.inventory_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Read-only API to inspect stock (to prove in a demo that stock went down, or did NOT go down after a failure).
 * Swagger UI: http://localhost:8083/swagger-ui.html   |   Through gateway: http://localhost:9090/inventory
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ProductRepository productRepository;

    /** GET /inventory -> all products and stock. */
    @GetMapping
    public List<Product> all() {
        return productRepository.findAll();
    }

    /** GET /inventory/{productId} -> one product, or 404. */
    @GetMapping("/{productId}")
    public ResponseEntity<Product> one(@PathVariable String productId) {
        return productRepository.findById(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
