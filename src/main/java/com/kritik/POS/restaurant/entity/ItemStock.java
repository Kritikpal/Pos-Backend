package com.kritik.POS.restaurant.entity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "item_stock", indexes = {
        @Index(name = "idx_item_stock_restaurant", columnList = "restaurant_id")
})
public class ItemStock {
    @Id
    private String sku;

    @OneToOne
    @JoinColumn(nullable = false)
    private MenuItem menuItem;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(nullable = false)
    private Integer totalStock;

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
