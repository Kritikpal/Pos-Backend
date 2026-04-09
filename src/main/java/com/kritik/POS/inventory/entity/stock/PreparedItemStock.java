package com.kritik.POS.inventory.entity.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "prepared_item_stock", indexes = {
        @Index(name = "idx_prepared_stock_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_prepared_stock_menu_item", columnList = "menu_item_id")
})
public class PreparedItemStock {

    // 🔑 One row per menu item (per restaurant)
    @Id
    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    // 📦 Ready-to-sell quantity
    @Column(name = "available_qty", nullable = false)
    private Double availableQty = 0.0;

    // 🔒 Reserved (optional for future use like pending orders)
    @Column(name = "reserved_qty", nullable = false)
    private Double reservedQty = 0.0;

    // 🍽 Unit of sale (PLATE, GLASS, PCS)
    @Column(name = "unit_code", nullable = false, length = 30)
    private String unitCode;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 🕒 Lifecycle hooks
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.availableQty == null) this.availableQty = 0.0;
        if (this.reservedQty == null) this.reservedQty = 0.0;
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}