package com.example.inventory_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.inventory_service.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    /** "Did we already handle this saga?" (idempotency check). */
    Optional<Reservation> findBySagaId(String sagaId);
}
