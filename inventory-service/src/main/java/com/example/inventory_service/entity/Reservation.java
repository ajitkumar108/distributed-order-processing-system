package com.example.inventory_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.inventory_service.enums.ReservationStatus;

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

/** Record of one reservation attempt (table "reservations") - the audit trail + idempotency guard. */
@Entity
@Table(name = "reservations")
@Getter
 @Setter
  @NoArgsConstructor
   @AllArgsConstructor
    @Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UNIQUE: one reservation per saga. A duplicate request finds this row and does not decrement stock twice. */
    @Column(nullable = false, unique = true)
    private String sagaId;

    private String orderId;
    private String productId;
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    /** Why it failed (empty when RESERVED). */
    private String reason;

    private LocalDateTime createdAt;
}
