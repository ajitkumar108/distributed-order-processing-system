package com.example.distributed_order_processing_system.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.distributed_order_processing_system.enums.OrderStatus;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "orders")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
@Builder

public class Order {
    @Id 
    @GeneratedValue 
    private UUID id;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;

}
