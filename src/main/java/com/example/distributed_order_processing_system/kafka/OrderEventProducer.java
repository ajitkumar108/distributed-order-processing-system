package com.example.distributed_order_processing_system.kafka;

import com.example.distributed_order_processing_system.events.OrderSagaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderSagaEvent> kafkaTemplate;

    private static final String PAYMENT_REQUEST_TOPIC =
            "payment-requests";

    public void publishPaymentRequest(OrderSagaEvent event) {

        kafkaTemplate.send(
                PAYMENT_REQUEST_TOPIC,
                event
        ).whenComplete((result, ex) -> {

            if (ex != null) {

                System.out.println("KAFKA SEND FAILED");
                ex.printStackTrace();

            } else {

                System.out.println("KAFKA SEND SUCCESS");
                System.out.println(
                        result.getRecordMetadata()
                );
            }
        });
    }
}