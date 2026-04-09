package com.kritik.POS.inventory.entity.stock;
import com.kritik.POS.inventory.entity.stockEntry.Supplier;
import com.kritik.POS.restaurant.entity.MenuItem;
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

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(nullable = false)
    private Integer totalStock;

    @Column(nullable = false)
    private Integer reorderLevel = 0;

    @Column(nullable = false, length = 30)
    private String unitOfMeasure = "unit";

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

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
