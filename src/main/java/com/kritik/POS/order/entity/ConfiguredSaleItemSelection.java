package com.kritik.POS.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Entity
@Table(name = "configured_sale_item_selection")
public class ConfiguredSaleItemSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "configured_sale_item_id", nullable = false)
    @ToString.Exclude
    private ConfiguredSaleItem configuredSaleItem;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "slot_name", nullable = false)
    private String slotName;

    @Column(name = "child_menu_item_id", nullable = false)
    private Long childMenuItemId;

    @Column(name = "child_item_name", nullable = false)
    private String childItemName;

    @Column(name = "quantity", nullable = false, columnDefinition = "integer default 1")
    private Integer quantity = 1;

    @Column(name = "price_delta", nullable = false)
    private BigDecimal priceDelta;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = createdAt;
    }

    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        ConfiguredSaleItemSelection that = (ConfiguredSaleItemSelection) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
