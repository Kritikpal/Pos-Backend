package com.kritik.POS.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "configured_sale_item")
public class ConfiguredSaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    @Column(name = "configured_template_id", nullable = false)
    private Long configuredTemplateId;

    @Column(name = "parent_menu_item_id", nullable = false)
    private Long parentMenuItemId;

    @Column(name = "line_name", nullable = false)
    private String lineName;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "tax_class_code_snapshot", nullable = false)
    private String taxClassCodeSnapshot;

    @Column(name = "price_includes_tax", nullable = false)
    private boolean priceIncludesTax = false;

    @Column(name = "unit_list_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitListAmount;

    @Column(name = "unit_discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitDiscountAmount;

    @Column(name = "unit_taxable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitTaxableAmount;

    @Column(name = "unit_tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitTaxAmount;

    @Column(name = "unit_total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitTotalAmount;

    @Column(name = "line_subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineSubtotalAmount;

    @Column(name = "line_discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineDiscountAmount;

    @Column(name = "line_taxable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTaxableAmount;

    @Column(name = "line_tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTaxAmount;

    @Column(name = "line_total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotalAmount;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @OneToMany(mappedBy = "configuredSaleItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("id asc")
    @ToString.Exclude
    private List<ConfiguredSaleItemSelection> selections = new ArrayList<>();

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
        ConfiguredSaleItem that = (ConfiguredSaleItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
