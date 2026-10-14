package com.example.distributed_order_processing_system.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.distributed_order_processing_system.dto.OrderRequest;
import com.example.distributed_order_processing_system.entity.Order;
import com.example.distributed_order_processing_system.service.OrderService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag (name = "order APIs", description = "Order Management System")
public class OrderController {

    
    private final OrderService orderService;

    
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody OrderRequest request) {

        return ResponseEntity.ok(
                orderService.createOrder(request)
        );
    }
}