package com.kritik.POS.inventory.entity.production;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "production_entry_item", indexes = {
        @Index(name = "idx_production_entry_item_entry", columnList = "production_entry_id"),
        @Index(name = "idx_production_entry_item_sku", columnList = "ingredient_sku")
})
public class ProductionEntryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // 🔗 Link to ProductionEntry (HEADER)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_entry_id", nullable = false)
    private ProductionEntry productionEntry;

    // 🔑 Ingredient reference
    @Column(name = "ingredient_sku", nullable = false, length = 100)
    private String ingredientSku;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    // 📉 Actual deducted quantity from stock
    @Column(name = "deducted_qty", nullable = false)
    private Double deductedQty;

    // 🧪 Unit of ingredient (KG, GRAM, LITRE, ML, PCS)
    @Column(name = "unit_code", nullable = false, length = 30)
    private String unitCode;
}