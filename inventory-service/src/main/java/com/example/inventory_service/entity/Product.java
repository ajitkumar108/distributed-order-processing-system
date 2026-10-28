package com.example.inventory_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A product and how many units are in stock (table "products"). */
@Entity
@Table(name = "products")
@Getter
@Setter
 @NoArgsConstructor
  @AllArgsConstructor
public class Product {

    /** Business key such as "PROD-1" (this is what Order Service sends as productId). */
    @Id
    private String productId;

    private String name;

    /** Units available right now. */
    private int stock;
}
