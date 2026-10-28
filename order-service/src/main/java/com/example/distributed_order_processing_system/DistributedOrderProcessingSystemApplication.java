package com.example.distributed_order_processing_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ORDER SERVICE = the SAGA ORCHESTRATOR.
 *
 * It owns the order and decides, step by step, what happens next:
 *   Order created -> ask Payment -> ask Inventory -> done
 * and if something fails after money was taken, it asks Payment to refund (compensation).
 *
 * Payment and Inventory never talk to each other; they only answer Order Service through Kafka.
 */
@SpringBootApplication
@EnableScheduling   // Needed so the @Scheduled recovery job (SagaRecoveryJob) actually runs.
public class DistributedOrderProcessingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedOrderProcessingSystemApplication.class, args);
    }
}
