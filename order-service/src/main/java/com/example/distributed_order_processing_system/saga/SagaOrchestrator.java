package com.example.distributed_order_processing_system.saga;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.distributed_order_processing_system.entity.Order;
import com.example.distributed_order_processing_system.enums.OrderStatus;
import com.example.distributed_order_processing_system.events.EventTypes;
import com.example.distributed_order_processing_system.events.OrderSagaEvent;
import com.example.distributed_order_processing_system.events.Topics;
import com.example.distributed_order_processing_system.kafka.SagaEventPublisher;
import com.example.distributed_order_processing_system.repository.OrderRepository;
import com.example.distributed_order_processing_system.repository.SagaStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * THE BRAIN OF THE PROJECT: the saga orchestrator (state machine).
 *
 * Every handler follows the same 4 steps:
 *   1. load the saga row
 *   2. CHECK it is in the state we expect  -> otherwise it's a duplicate/late message: ignore it (IDEMPOTENCY)
 *   3. change state (saga + order)
 *   4. publish the next request, if any
 *
 * Why the check in step 2 matters: Kafka guarantees "at least once" delivery, so the same message can arrive
 * twice. Processing it twice must not break anything.
 *
 * @Transactional = DB changes of the method happen together or not at all.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final SagaEventPublisher publisher;

    // =====================================================================================
    //  START
    // =====================================================================================

    /** Creates the saga for a freshly saved order and asks Payment Service to charge the customer. */
    @Transactional
    public SagaState startSaga(Order order) {
        LocalDateTime now = LocalDateTime.now();

        SagaState saga = SagaState.builder()
                .sagaId(UUID.randomUUID())          // new id that identifies this whole transaction
                .orderId(order.getId())
                .status(SagaStatus.PAYMENT_PENDING)  // first step: waiting for payment
                .lastEvent(EventTypes.PAYMENT_REQUESTED)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        sagaStateRepository.save(saga);

        publisher.publish(Topics.PAYMENT_REQUESTS, buildRequest(saga, order, EventTypes.PAYMENT_REQUESTED));
        log.info("SAGA STARTED saga={} order={}", saga.getSagaId(), order.getId());
        return saga;
    }

    // =====================================================================================
    //  RESULTS FROM PAYMENT SERVICE
    // =====================================================================================

    /** Payment succeeded -> next step: reserve stock. */
    @Transactional
    public void onPaymentCompleted(OrderSagaEvent event) {
        SagaState saga = findSaga(event);
        if (!isExpected(saga, SagaStatus.PAYMENT_PENDING, event)) {
            return; // duplicate or late event
        }
        Order order = findOrder(saga);

        moveTo(saga, SagaStatus.INVENTORY_PENDING, EventTypes.PAYMENT_COMPLETED);
        publisher.publish(Topics.INVENTORY_REQUESTS, buildRequest(saga, order, EventTypes.INVENTORY_RESERVE_REQUESTED));
    }

    /**
     * Payment failed -> the saga ends. NOTE: this is plain FAILURE HANDLING, not compensation:
     * the payment step did not succeed, so there is nothing to undo.
     */
    @Transactional
    public void onPaymentFailed(OrderSagaEvent event) {
        SagaState saga = findSaga(event);
        if (!isExpected(saga, SagaStatus.PAYMENT_PENDING, event)) {
            return;
        }
        Order order = findOrder(saga);

        moveTo(saga, SagaStatus.FAILED, EventTypes.PAYMENT_FAILED);
        setOrderStatus(order, OrderStatus.FAILED);
        log.info("SAGA FAILED (payment) saga={}", saga.getSagaId());
    }

    /** Refund finished -> compensation complete, the order is cancelled. */
    @Transactional
    public void onPaymentRefunded(OrderSagaEvent event) {
        SagaState saga = findSaga(event);
        if (!isExpected(saga, SagaStatus.COMPENSATING, event)) {
            return;
        }
        Order order = findOrder(saga);

        moveTo(saga, SagaStatus.COMPENSATED, EventTypes.PAYMENT_REFUNDED);
        setOrderStatus(order, OrderStatus.COMPENSATED);
        log.info("SAGA COMPENSATED saga={}", saga.getSagaId());
    }

    // =====================================================================================
    //  RESULTS FROM INVENTORY SERVICE
    // =====================================================================================

    /** Stock reserved -> everything succeeded. */
    @Transactional
    public void onInventoryReserved(OrderSagaEvent event) {
        SagaState saga = findSaga(event);
        if (!isExpected(saga, SagaStatus.INVENTORY_PENDING, event)) {
            return;
        }
        Order order = findOrder(saga);

        moveTo(saga, SagaStatus.COMPLETED, EventTypes.INVENTORY_RESERVED);
        setOrderStatus(order, OrderStatus.CONFIRMED);
        log.info("SAGA COMPLETED saga={}", saga.getSagaId());
    }

    /**
     * Stock could NOT be reserved, but the customer was ALREADY charged.
     * This is where COMPENSATION is needed: undo the payment by asking for a refund.
     */
    @Transactional
    public void onInventoryFailed(OrderSagaEvent event) {
        SagaState saga = findSaga(event);
        if (!isExpected(saga, SagaStatus.INVENTORY_PENDING, event)) {
            return;
        }
        Order order = findOrder(saga);
        startCompensation(saga, order, EventTypes.INVENTORY_FAILED);
    }

    // =====================================================================================
    //  RECOVERY SUPPORT (called by SagaRecoveryJob)
    // =====================================================================================

    /** Re-send the request of the step the saga is currently waiting for (the answer never arrived). */
    @Transactional
    public void retry(SagaState saga) {
        Order order = findOrder(saga);

        saga.setRetryCount(saga.getRetryCount() + 1);
        saga.setUpdatedAt(LocalDateTime.now());   // so we don't retry again immediately
        sagaStateRepository.save(saga);

        switch (saga.getStatus()) {
            case PAYMENT_PENDING   -> publisher.publish(Topics.PAYMENT_REQUESTS,
                    buildRequest(saga, order, EventTypes.PAYMENT_REQUESTED));
            case INVENTORY_PENDING -> publisher.publish(Topics.INVENTORY_REQUESTS,
                    buildRequest(saga, order, EventTypes.INVENTORY_RESERVE_REQUESTED));
            case COMPENSATING      -> publisher.publish(Topics.PAYMENT_REQUESTS,
                    buildRequest(saga, order, EventTypes.PAYMENT_REFUND_REQUESTED));
            default -> { /* terminal states are never retried */ }
        }
        log.warn("SAGA RETRY #{} saga={} status={}", saga.getRetryCount(), saga.getSagaId(), saga.getStatus());
    }

    /** Retries are used up. Decide the safest end state for the saga. */
    @Transactional
    public void giveUp(SagaState saga) {
        Order order = findOrder(saga);

        switch (saga.getStatus()) {
            case PAYMENT_PENDING -> {
                // Payment never answered. We never saw a success, so end as FAILED.
                moveTo(saga, SagaStatus.FAILED, "GAVE_UP_WAITING_FOR_PAYMENT");
                setOrderStatus(order, OrderStatus.FAILED);
            }
            case INVENTORY_PENDING ->
                // Money may have been taken but stock never answered -> undo the payment.
                startCompensation(saga, order, "GAVE_UP_WAITING_FOR_INVENTORY");
            case COMPENSATING -> {
                // Even the refund is not confirming. Needs a human. Log loudly.
                moveTo(saga, SagaStatus.FAILED, "GAVE_UP_WAITING_FOR_REFUND");
                setOrderStatus(order, OrderStatus.FAILED);
                log.error("MANUAL INTERVENTION NEEDED: refund not confirmed for saga={} order={}",
                        saga.getSagaId(), order.getId());
            }
            default -> { /* terminal: nothing to do */ }
        }
    }

    // =====================================================================================
    //  HELPERS
    // =====================================================================================

    /** Moves saga + order into "compensating" and asks Payment Service for a refund. */
    private void startCompensation(SagaState saga, Order order, String reasonEvent) {
        moveTo(saga, SagaStatus.COMPENSATING, reasonEvent);
        setOrderStatus(order, OrderStatus.COMPENSATING);
        publisher.publish(Topics.PAYMENT_REQUESTS, buildRequest(saga, order, EventTypes.PAYMENT_REFUND_REQUESTED));
        log.warn("COMPENSATION STARTED saga={} reason={}", saga.getSagaId(), reasonEvent);
    }

    /** Builds a request event carrying everything the other service needs (amount, product, quantity). */
    private OrderSagaEvent buildRequest(SagaState saga, Order order, String eventType) {
        Map<String, Object> payload = Map.of(
                "amount", order.getAmount(),
                "productId", order.getProductId(),
                "quantity", order.getQuantity());
        return OrderSagaEvent.of(saga.getSagaId().toString(), order.getId().toString(), eventType, payload);
    }

    /** Updates the saga row: new state + audit info. Resets retryCount because we made progress. */
    private void moveTo(SagaState saga, SagaStatus newStatus, String lastEvent) {
        saga.setStatus(newStatus);
        saga.setLastEvent(lastEvent);
        saga.setRetryCount(0);
        saga.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(saga);
    }

    private void setOrderStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        orderRepository.save(order);
    }

    /** Idempotency guard: true only if the saga exists AND is currently in the state this event belongs to. */
    private boolean isExpected(SagaState saga, SagaStatus expected, OrderSagaEvent event) {
        if (saga == null) {
            log.warn("Unknown saga {} for event {}", event.getSagaId(), event.getEventType());
            return false;
        }
        if (saga.getStatus() != expected) {
            log.info("IGNORED duplicate/late event {} (saga {} is {}, expected {})",
                    event.getEventType(), saga.getSagaId(), saga.getStatus(), expected);
            return false;
        }
        return true;
    }

    private SagaState findSaga(OrderSagaEvent event) {
        return sagaStateRepository.findById(UUID.fromString(event.getSagaId())).orElse(null);
    }

    private Order findOrder(SagaState saga) {
        return orderRepository.findById(saga.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order missing for saga " + saga.getSagaId()));
    }
}
