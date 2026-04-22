package com.kritik.POS.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "sale_item", indexes = {
        @Index(name = "idx_sale_item_restaurant", columnList = "restaurant_id")
})
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long saleItemId;

    @Column(nullable = false,updatable = false)
    private String saleItemName;

    @Column(nullable = false, updatable = false)
    private BigDecimal saleItemPrice;

    @Column(name = "tax_class_code_snapshot", nullable = false, updatable = false)
    private String taxClassCodeSnapshot;

    @Column(name = "price_includes_tax", nullable = false, updatable = false)
    private boolean priceIncludesTax = false;

    @Column(name = "unit_list_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal unitListAmount;

    @Column(name = "unit_discount_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal unitDiscountAmount;

    @Column(name = "unit_taxable_amount", nullable = false, updatable = false, precision = 19, scale = 2)
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

    @Column(nullable = false,updatable = false)
    private Integer amount;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @ManyToOne
    @JoinColumn(nullable = false,updatable = false)
    private Order order;

    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean isDeleted = false;

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
