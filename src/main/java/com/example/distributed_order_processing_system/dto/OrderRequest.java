package com.example.distributed_order_processing_system.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data 
public class OrderRequest {
    private Long productId;

    private Integer quantity;

    private BigDecimal amount;
}
