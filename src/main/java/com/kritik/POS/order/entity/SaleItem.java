package com.kritik.POS.order.entity;

import com.kritik.POS.restaurant.entity.MenuItem;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sale_item", indexes = {
        @Index(name = "idx_sale_item_restaurant", columnList = "restaurant_id")
})
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long saleItemId;

    @Column(nullable = false,updatable = false)
    private String saleItemName;

    @Column(nullable = false, updatable = false)
    private Double saleItemPrice;

    @Column(nullable = false,updatable = false)
    private Integer amount;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @ManyToOne
    @JoinColumn(nullable = false,updatable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "manu_itemId")
    private MenuItem menuItem;

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
