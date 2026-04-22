package com.kritik.POS.order.entity;

import com.kritik.POS.tax.api.TaxCalculationMode;
import com.kritik.POS.tax.api.TaxCompoundMode;
import com.kritik.POS.tax.api.TaxValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Data
@Table(name = "order_line_tax")
public class OrderLineTax {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_item_id")
    private SaleItem saleItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configured_sale_item_id")
    private ConfiguredSaleItem configuredSaleItem;

    @Column(name = "reference_key", nullable = false)
    private String referenceKey;

    @Column(name = "tax_definition_code", nullable = false)
    private String taxDefinitionCode;

    @Column(name = "tax_display_name", nullable = false)
    private String taxDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private TaxValueType valueType;

    @Column(name = "rate_or_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal rateOrAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_mode", nullable = false, length = 20)
    private TaxCalculationMode calculationMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "compound_mode", nullable = false, length = 30)
    private TaxCompoundMode compoundMode;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "taxable_base_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxableBaseAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "jurisdiction_country_code")
    private String jurisdictionCountryCode;

    @Column(name = "jurisdiction_region_code")
    private String jurisdictionRegionCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
