package com.kritik.POS.tax.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "tax_rate", indexes = {
        @Index(name = "idx_tax_rate_restaurant", columnList = "restaurant_id")
})
public class TaxRate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long taxId;

    @Column(nullable = false)
    private String taxName;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(nullable = false)
    private Double taxAmount;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean isDeleted = false;

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
