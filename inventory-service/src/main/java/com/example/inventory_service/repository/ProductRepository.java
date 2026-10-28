package com.example.inventory_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.inventory_service.entity.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * SELECT ... FOR UPDATE: locks the product row until our transaction ends.
     * Without the lock, two orders processed at the same moment could both read "stock = 1"
     * and both succeed (overselling). With the lock, the second waits for the first.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") String productId);
}
