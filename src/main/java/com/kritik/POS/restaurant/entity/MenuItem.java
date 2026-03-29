package com.kritik.POS.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kritik.POS.order.entity.SaleItem;
import jakarta.persistence.*;
import lombok.Data;

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

    @Column(columnDefinition = "TEXT")
    private String description;

    @JoinColumn(nullable = false, name = "priceId")
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private ItemPrice itemPrice;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "categoryId", nullable = false)
    private Category category;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false)
    private Boolean isTrending = false;

    @OneToMany(mappedBy = "menuItem")
    @JsonIgnore
    private List<SaleItem> saleItems;

    @OneToOne(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(nullable = false)
    private ItemStock itemStock;

    @OneToOne
    @JoinColumn
    private ProductFile productImage;

    @Column(name = "has_recipi")
    private Boolean hasRecipe = false;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MenuItemIngredient> ingredientUsages = new ArrayList<>();

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
