package com.kritik.POS.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "restaurant_table", indexes = {
        @Index(name = "idx_restaurant_table_restaurant", columnList = "restaurant_id")
})
public class RestaurantTable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long tableId;

    @Column(name = "table_number", unique = true, nullable = false)
    private Integer tableNumber = 1;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(nullable = false)
    private Integer seats = 1;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
