package com.kritik.POS.inventory.entity;

import com.kritik.POS.restaurant.entity.MenuItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "menu_item_ingredient", indexes = {
        @Index(name = "idx_menu_item_ingredient_menu", columnList = "menu_item_id"),
        @Index(name = "idx_menu_item_ingredient_sku", columnList = "ingredient_sku")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_menu_item_ingredient", columnNames = {"menu_item_id", "ingredient_sku"})
})
public class MenuItemIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_sku", nullable = false)
    private IngredientStock ingredientStock;

    @Column(name = "quantity_required", nullable = false)
    private Double quantityRequired;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
