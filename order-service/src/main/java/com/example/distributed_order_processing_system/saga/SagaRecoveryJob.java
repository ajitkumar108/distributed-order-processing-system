package com.example.distributed_order_processing_system.saga;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.distributed_order_processing_system.repository.SagaStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CRASH RECOVERY.
 *
 * Problem: Order Service may crash, Payment Service may be down, or a Kafka message may be lost.
 * The saga then sits forever in PAYMENT_PENDING / INVENTORY_PENDING / COMPENSATING.
 *
 * Solution: at startup AND every 30 seconds, look in MySQL for sagas that are not finished and have not
 * changed for a while, and re-send their current request. Because every consumer is idempotent, re-sending is safe.
 * After too many retries we give up in the safest way (see SagaOrchestrator.giveUp).
 *
 * Limitation: if you run several Order Service instances they would all run this job.
 * Harmless here (idempotent), but a production system would add a distributed lock (e.g. ShedLock).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaRecoveryJob {

    private final SagaStateRepository sagaStateRepository;
    private final SagaOrchestrator orchestrator;

    /** A saga untouched for this long (seconds) is considered stuck. */
    @Value("${saga.recovery.stuck-after-seconds:60}")
    private long stuckAfterSeconds;

    /** After this many re-sends we stop retrying and call giveUp(). */
    @Value("${saga.recovery.max-retries:5}")
    private int maxRetries;

    /** Runs once, right after the application finished starting (this is the "resume after crash" moment). */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverOnStartup() {
        log.info("Running saga recovery at startup");
        recover();
    }

    /** Runs repeatedly: 30 seconds after the previous run finished (configurable). */
    @Scheduled(fixedDelayString = "${saga.recovery.interval-ms:30000}")
    @Transactional
    public void recoverPeriodically() {
        recover();
    }

    private void recover() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(stuckAfterSeconds);

        // All non-terminal sagas whose last update is older than the cutoff.
        List<SagaState> stuck = sagaStateRepository.findByStatusNotInAndUpdatedAtBefore(SagaStatus.TERMINAL, cutoff);

        for (SagaState saga : stuck) {
            if (saga.getRetryCount() >= maxRetries) {
                orchestrator.giveUp(saga);   // stop retrying, choose safest end state
            } else {
                orchestrator.retry(saga);    // re-send the current request
            }
        }
        if (!stuck.isEmpty()) {
            log.warn("Recovery processed {} stuck saga(s)", stuck.size());
        }
    }
}
