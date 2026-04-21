package com.kritik.POS.inventory.entity.recipi;

import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.restaurant.entity.MenuItem;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MenuRecipe recipe;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_sku", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
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
