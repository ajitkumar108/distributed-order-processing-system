package com.example.distributed_order_processing_system.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.distributed_order_processing_system.enums.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The customer's order (table "orders").
 * Table is named "orders" because ORDER is a reserved word in SQL.
 */
@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    /** Auto-generated random UUID primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Which product is being bought (must exist in Inventory Service). */
    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    /** Money amount. BigDecimal (never double!) because floating point cannot represent money exactly. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /**
     * Optional client-supplied "Idempotency-Key". UNIQUE, so the same key can never create two orders
     * (protects against the client retrying a POST after a timeout).
     */
    @Column(unique = true)
    private String idempotencyKey;

    private LocalDateTime createdAt;
}
