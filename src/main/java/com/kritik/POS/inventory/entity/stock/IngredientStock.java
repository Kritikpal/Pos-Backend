package com.kritik.POS.inventory.entity.stock;

import com.kritik.POS.inventory.entity.master.Ingredient;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.stockEntry.Supplier;
import com.kritik.POS.inventory.entity.unit.UnitMaster;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "ingredient_stock", indexes = {
        @Index(name = "idx_ingredient_stock_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_ingredient_stock_name", columnList = "ingredient_name")
})
public class IngredientStock implements Persistable<String> {
    @Id
    private String sku;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku", referencedColumnName = "sku", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Ingredient ingredient;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String category;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
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
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MenuItemIngredient> menuItemIngredients = new ArrayList<>();

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() {
        return sku;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PostPersist
    @PostLoad
    public void markNotNew() {
        isNew = false;
    }

    @Transient
    public Ingredient getResolvedIngredient() {
        return ingredient;
    }

    public String getIngredientName() {
        if (isIngredientInitialized() && ingredient.getIngredientName() != null) {
            return ingredient.getIngredientName();
        }
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getDescription() {
        if (isIngredientInitialized() && ingredient.getDescription() != null) {
            return ingredient.getDescription();
        }
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        if (isIngredientInitialized() && ingredient.getCategory() != null) {
            return ingredient.getCategory();
        }
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getRestaurantId() {
        if (isIngredientInitialized() && ingredient.getRestaurantId() != null) {
            return ingredient.getRestaurantId();
        }
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Supplier getSupplier() {
        if (isIngredientInitialized()) {
            Supplier ingredientSupplier = ingredient.getSupplier();
            if (ingredientSupplier != null && Hibernate.isInitialized(ingredientSupplier)) {
                return ingredientSupplier;
            }
        }
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Boolean getIsActive() {
        if (isIngredientInitialized() && ingredient.getIsActive() != null) {
            return ingredient.getIsActive();
        }
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getIsDeleted() {
        if (isIngredientInitialized() && ingredient.getIsDeleted() != null) {
            return ingredient.getIsDeleted();
        }
        return isDeleted;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    @Transient
    public UnitMaster getBaseUnit() {
        if (!isIngredientInitialized()) {
            return null;
        }
        UnitMaster baseUnit = ingredient.getBaseUnit();
        return baseUnit != null && Hibernate.isInitialized(baseUnit) ? baseUnit : null;
    }

    public String getUnitOfMeasure() {
        UnitMaster baseUnit = getBaseUnit();
        if (baseUnit != null && baseUnit.getCode() != null) {
            return baseUnit.getCode();
        }
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    @Transient
    private boolean isIngredientInitialized() {
        return ingredient != null && Hibernate.isInitialized(ingredient);
    }
}
