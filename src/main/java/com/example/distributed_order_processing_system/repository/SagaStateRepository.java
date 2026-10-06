package com.example.distributed_order_processing_system.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.distributed_order_processing_system.saga.SagaState;

public interface SagaStateRepository extends JpaRepository <SagaState , UUID> {
    
}
