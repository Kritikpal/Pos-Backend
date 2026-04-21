package com.kritik.POS.tax.entity;

import com.kritik.POS.tax.entity.enums.TaxCalculationMode;
import com.kritik.POS.tax.entity.enums.TaxCompoundMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Data
@Table(name = "tax_rule", indexes = {
        @Index(name = "idx_tax_rule_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_tax_rule_definition", columnList = "tax_definition_id"),
        @Index(name = "idx_tax_rule_tax_class", columnList = "tax_class_id")
})
public class TaxRule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "tax_definition_id", nullable = false)
    private Long taxDefinitionId;

    @Column(name = "tax_class_id", nullable = false)
    private Long taxClassId;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_mode", nullable = false, length = 20)
    private TaxCalculationMode calculationMode = TaxCalculationMode.EXCLUSIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "compound_mode", nullable = false, length = 30)
    private TaxCompoundMode compoundMode = TaxCompoundMode.BASE_ONLY;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo = 1;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "region_code", length = 20)
    private String regionCode;

    @Column(name = "buyer_tax_category", length = 60)
    private String buyerTaxCategory;

    @Column(name = "min_amount", precision = 19, scale = 4)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

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
