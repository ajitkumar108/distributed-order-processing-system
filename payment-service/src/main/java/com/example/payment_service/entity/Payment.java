package com.example.payment_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.payment_service.enums.PaymentStatus;

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

/** One payment attempt (table "payments" in payment_db). */
@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String orderId;

    /**
     * UNIQUE: only one payment per saga can ever exist. This is what makes processing IDEMPOTENT -
     * if the same PAYMENT_REQUESTED message arrives twice, the second time we find this row and
     * do NOT charge again.
     */
    @Column(nullable = false, unique = true)
    private String sagaId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Fake id of the (simulated) payment provider transaction. */
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
