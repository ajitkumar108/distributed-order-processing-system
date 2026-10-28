package com.example.distributed_order_processing_system.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.distributed_order_processing_system.entity.Order;
import com.example.distributed_order_processing_system.enums.OrderStatus;
import com.example.distributed_order_processing_system.events.EventTypes;
import com.example.distributed_order_processing_system.events.OrderSagaEvent;
import com.example.distributed_order_processing_system.events.Topics;
import com.example.distributed_order_processing_system.kafka.SagaEventPublisher;
import com.example.distributed_order_processing_system.repository.OrderRepository;
import com.example.distributed_order_processing_system.repository.SagaStateRepository;

/**
 * Unit tests of the state machine. No Spring, no Kafka, no database: repositories and publisher are MOCKS
 * (fake objects), so the tests run in milliseconds and check only the decision logic.
 */
@ExtendWith(MockitoExtension.class)
class SagaOrchestratorTest {

    @Mock SagaStateRepository sagaRepo;
    @Mock OrderRepository orderRepo;
    @Mock SagaEventPublisher publisher;

    // Creates the real orchestrator and injects the three mocks above into its constructor.
    @InjectMocks SagaOrchestrator orchestrator;

    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private SagaState saga(SagaStatus status) {
        return SagaState.builder().sagaId(sagaId).orderId(orderId).status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private Order order() {
        return Order.builder().id(orderId).productId("PROD-1").quantity(2)
                .amount(new BigDecimal("100.00")).status(OrderStatus.PENDING).build();
    }

    private OrderSagaEvent event(String type) {
        return OrderSagaEvent.of(sagaId.toString(), orderId.toString(), type, Map.of());
    }

    @Test
    void paymentCompleted_requestsInventory() {
        SagaState saga = saga(SagaStatus.PAYMENT_PENDING);
        when(sagaRepo.findById(sagaId)).thenReturn(Optional.of(saga));
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order()));

        orchestrator.onPaymentCompleted(event(EventTypes.PAYMENT_COMPLETED));

        assertEquals(SagaStatus.INVENTORY_PENDING, saga.getStatus());
        verify(publisher).publish(eq(Topics.INVENTORY_REQUESTS),
                argThat(e -> EventTypes.INVENTORY_RESERVE_REQUESTED.equals(e.getEventType())));
    }

    @Test
    void paymentFailed_failsOrder_withoutCompensation() {
        SagaState saga = saga(SagaStatus.PAYMENT_PENDING);
        Order order = order();
        when(sagaRepo.findById(sagaId)).thenReturn(Optional.of(saga));
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        orchestrator.onPaymentFailed(event(EventTypes.PAYMENT_FAILED));

        assertEquals(SagaStatus.FAILED, saga.getStatus());
        assertEquals(OrderStatus.FAILED, order.getStatus());
        verifyNoInteractions(publisher); // nothing succeeded, so nothing to compensate
    }

    @Test
    void inventoryReserved_confirmsOrder() {
        SagaState saga = saga(SagaStatus.INVENTORY_PENDING);
        Order order = order();
        when(sagaRepo.findById(sagaId)).thenReturn(Optional.of(saga));
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        orchestrator.onInventoryReserved(event(EventTypes.INVENTORY_RESERVED));

        assertEquals(SagaStatus.COMPLETED, saga.getStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void inventoryFailed_startsCompensation_refundRequested() {
        SagaState saga = saga(SagaStatus.INVENTORY_PENDING);
        Order order = order();
        when(sagaRepo.findById(sagaId)).thenReturn(Optional.of(saga));
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        orchestrator.onInventoryFailed(event(EventTypes.INVENTORY_FAILED));

        assertEquals(SagaStatus.COMPENSATING, saga.getStatus());
        assertEquals(OrderStatus.COMPENSATING, order.getStatus());
        verify(publisher).publish(eq(Topics.PAYMENT_REQUESTS),
                argThat(e -> EventTypes.PAYMENT_REFUND_REQUESTED.equals(e.getEventType())));
    }

    @Test
    void refunded_completesCompensation() {
        SagaState saga = saga(SagaStatus.COMPENSATING);
        Order order = order();
        when(sagaRepo.findById(sagaId)).thenReturn(Optional.of(saga));
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        orchestrator.onPaymentRefunded(event(EventTypes.PAYMENT_REFUNDED));

        assertEquals(SagaStatus.COMPENSATED, saga.getStatus());
        assertEquals(OrderStatus.COMPENSATED, order.getStatus());
    }

    @Test
    void duplicateEvent_isIgnored_idempotency() {
        // Saga is already COMPLETED; a late/duplicate PAYMENT_COMPLETED arrives.
        SagaState saga = saga(SagaStatus.COMPLETED);
        when(sagaRepo.findById(sagaId)).thenReturn(Optional.of(saga));

        orchestrator.onPaymentCompleted(event(EventTypes.PAYMENT_COMPLETED));

        assertEquals(SagaStatus.COMPLETED, saga.getStatus());   // unchanged
        verifyNoInteractions(publisher);                         // nothing re-published
    }
}
