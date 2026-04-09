package com.kritik.POS.inventory.entity.production;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "production_entry", indexes = {
        @Index(name = "idx_production_entry_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_production_entry_menu_item", columnList = "menu_item_id"),
        @Index(name = "idx_production_entry_time", columnList = "production_time")
})
public class ProductionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "produced_qty", nullable = false)
    private Double producedQty;

    @Column(name = "unit_code", nullable = false, length = 30)
    private String unitCode; // PLATE, GLASS, PCS

    @Column(name = "recipe_batch_id")
    private Long recipeBatchId;

    @Column(name = "production_time", nullable = false)
    private LocalDateTime productionTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.productionTime == null) {
            this.productionTime = now;
        }
        this.createdAt = now;
    }
}