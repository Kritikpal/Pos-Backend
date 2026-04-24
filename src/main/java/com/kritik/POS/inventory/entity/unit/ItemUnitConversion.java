package com.kritik.POS.inventory.entity.unit;

import com.kritik.POS.inventory.entity.enums.UnitConversionSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "item_unit_conversion",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_item_unit_conversion_source_unit",
                        columnNames = {"restaurant_id", "source_type", "source_id", "unit_id"}
                )
        },
        indexes = {
                @Index(name = "idx_item_unit_conversion_source", columnList = "restaurant_id,source_type,source_id"),
                @Index(name = "idx_item_unit_conversion_source_active", columnList = "restaurant_id,source_type,source_id,active")
        })
public class ItemUnitConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private UnitConversionSourceType sourceType;

    @Column(name = "source_id", nullable = false, length = 255)
    private String sourceId;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UnitMaster unit;

    @Column(name = "factor_to_base", nullable = false, precision = 19, scale = 6)
    private BigDecimal factorToBase;

    @Column(name = "purchase_allowed", nullable = false)
    private Boolean purchaseAllowed = true;

    @Column(name = "sale_allowed", nullable = false)
    private Boolean saleAllowed = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
