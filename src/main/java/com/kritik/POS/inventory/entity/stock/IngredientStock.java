package com.kritik.POS.inventory.entity.stock;

import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.stockEntry.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "ingredient_stock", indexes = {
        @Index(name = "idx_ingredient_stock_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_ingredient_stock_name", columnList = "ingredient_name")
})
public class IngredientStock {
    @Id
    private String sku;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private Double totalStock = 0.0;

    @Column(nullable = false)
    private Double reorderLevel = 0.0;

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

    @OneToMany(mappedBy = "ingredientStock")
    private List<MenuItemIngredient> menuItemIngredients = new ArrayList<>();

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
