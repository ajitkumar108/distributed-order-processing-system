package com.example.inventory_service.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inventory_service.entity.Product;
import com.example.inventory_service.entity.Reservation;
import com.example.inventory_service.enums.ReservationStatus;
import com.example.inventory_service.events.OrderSagaEvent;
import com.example.inventory_service.repository.ProductRepository;
import com.example.inventory_service.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Business rules of stock reservation. Database only; Kafka is handled elsewhere. */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Try to reserve stock for an order. IDEMPOTENT per saga.
     * One transaction: either (stock decremented + reservation saved) or nothing.
     */
    @Transactional
    public Reservation reserve(OrderSagaEvent event) {

        // 1) Duplicate request? Return what we decided the first time.
        Optional<Reservation> existing = reservationRepository.findBySagaId(event.getSagaId());
        if (existing.isPresent()) {
            log.info("Reservation already exists for saga={} -> {}", event.getSagaId(), existing.get().getStatus());
            return existing.get();
        }

        // 2) Read request data from the event payload.
        String productId = String.valueOf(event.getPayload().get("productId"));
        int quantity = ((Number) event.getPayload().get("quantity")).intValue();

        // 3) Lock the product row so concurrent orders can't oversell.
        Optional<Product> productOpt = productRepository.findByIdForUpdate(productId);

        ReservationStatus status;
        String reason = null;

        if (productOpt.isEmpty()) {
            status = ReservationStatus.FAILED;
            reason = "Unknown product " + productId;
        } else if (productOpt.get().getStock() < quantity) {
            status = ReservationStatus.FAILED;
            reason = "Insufficient stock: requested " + quantity + ", available " + productOpt.get().getStock();
        } else {
            Product product = productOpt.get();
            product.setStock(product.getStock() - quantity);   // decrement (saved automatically at commit)
            status = ReservationStatus.RESERVED;
        }

        Reservation reservation = Reservation.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .productId(productId)
                .quantity(quantity)
                .status(status)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Reservation {} saga={} product={} qty={} {}", status, event.getSagaId(), productId, quantity,
                reason == null ? "" : "(" + reason + ")");
        return reservationRepository.save(reservation);
    }
}
