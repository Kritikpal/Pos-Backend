package com.kritik.POS.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(
        name = "category",
        indexes = {
                @Index(name = "idx_category_name", columnList = "categoryName"),
                @Index(name = "idx_category_restaurant", columnList = "restaurant_id")
        }
)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long categoryId;

    @Column(nullable = false)
    private String categoryName;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String categoryDescription;

    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private List<MenuItem> menuItems;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isDeleted = false;

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
