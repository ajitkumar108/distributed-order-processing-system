package com.example.distributed_order_processing_system.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.distributed_order_processing_system.events.OrderSagaEvent;
import com.example.distributed_order_processing_system.service.OrderService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class PaymentCompletedConsumer {
    
    private final OrderService orderService;
    @KafkaListener (
        topics = "payment-responses",
        groupId = "order-group"
    )
    public void consume(OrderSagaEvent event){

        System.out.println("=============================");
        System.out.println("PAYMENT_COMPLETED RECIEVED");

        System.out.println("OrderId : " + event.getOrderId());

        System.out.println("==============================");

        orderService.confirmOrder(event.getOrderId());

        System.out.println("ORDER CONFIRMED");
        
    }

}
