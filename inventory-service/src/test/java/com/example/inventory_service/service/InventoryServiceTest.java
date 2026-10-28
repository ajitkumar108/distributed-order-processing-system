package com.example.inventory_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.inventory_service.entity.Product;
import com.example.inventory_service.entity.Reservation;
import com.example.inventory_service.enums.ReservationStatus;
import com.example.inventory_service.events.EventTypes;
import com.example.inventory_service.events.OrderSagaEvent;
import com.example.inventory_service.repository.ProductRepository;
import com.example.inventory_service.repository.ReservationRepository;

/** Unit tests of the reservation rules (repositories are mocks). */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ReservationRepository reservationRepository;
    @InjectMocks InventoryService service;

    private OrderSagaEvent request(String productId, int quantity) {
        return OrderSagaEvent.of("saga-1", "order-1", EventTypes.INVENTORY_RESERVE_REQUESTED,
                Map.of("productId", productId, "quantity", quantity));
    }

    @Test
    void reserve_decrementsStock_whenEnoughAvailable() {
        Product product = new Product("PROD-1", "Laptop", 10);
        when(reservationRepository.findBySagaId("saga-1")).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate("PROD-1")).thenReturn(Optional.of(product));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = service.reserve(request("PROD-1", 3));

        assertEquals(ReservationStatus.RESERVED, result.getStatus());
        assertEquals(7, product.getStock());   // 10 - 3
    }

    @Test
    void reserve_fails_andKeepsStock_whenNotEnough() {
        Product product = new Product("PROD-2", "Phone", 5);
        when(reservationRepository.findBySagaId("saga-1")).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate("PROD-2")).thenReturn(Optional.of(product));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = service.reserve(request("PROD-2", 10));

        assertEquals(ReservationStatus.FAILED, result.getStatus());
        assertEquals(5, product.getStock());   // unchanged
    }

    @Test
    void reserve_fails_forUnknownProduct() {
        when(reservationRepository.findBySagaId("saga-1")).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate("NOPE")).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = service.reserve(request("NOPE", 1));

        assertEquals(ReservationStatus.FAILED, result.getStatus());
    }

    @Test
    void reserve_isIdempotent_duplicateRequestDoesNotDecrementTwice() {
        Reservation already = Reservation.builder().sagaId("saga-1").status(ReservationStatus.RESERVED).build();
        when(reservationRepository.findBySagaId("saga-1")).thenReturn(Optional.of(already));

        Reservation result = service.reserve(request("PROD-1", 3));

        assertEquals(ReservationStatus.RESERVED, result.getStatus());
        verify(productRepository, never()).findByIdForUpdate(any());   // stock untouched
    }
}
