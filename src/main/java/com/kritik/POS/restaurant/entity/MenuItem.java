package com.kritik.POS.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(
        name = "menu_item",
        indexes = {
                @Index(name = "idx_menu_item_name", columnList = "itemName"),
                @Index(name = "idx_menu_item_restaurant", columnList = "restaurant_id")
        }
)
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String itemName;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "tax_class_id")
    private Long taxClassId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JoinColumn(nullable = false, name = "priceId")
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ItemPrice itemPrice;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "categoryId", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false)
    private Boolean isTrending = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type", nullable = false, length = 30)
    private MenuType menuType = MenuType.DIRECT;

    @OneToOne(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ItemStock itemStock;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", referencedColumnName = "menu_item_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PreparedItemStock preparedItemStock;

    @OneToOne
    @JoinColumn
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductFile productImage;

    @OneToOne(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MenuRecipe recipe;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MenuItemIngredient> ingredientUsages = new ArrayList<>();

    @Version
    @Column(name = "item_version")
    private Long itemVersion = 0L;

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
