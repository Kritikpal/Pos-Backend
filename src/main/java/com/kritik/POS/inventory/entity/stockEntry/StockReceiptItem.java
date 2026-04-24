package com.kritik.POS.inventory.entity.stockEntry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.entity.enums.StockReceiptSkuType;
import com.kritik.POS.inventory.entity.unit.UnitMaster;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "stock_receipt_item", indexes = {
        @Index(name = "idx_stock_receipt_item_receipt", columnList = "receipt_id"),
        @Index(name = "idx_stock_receipt_item_item_sku", columnList = "item_stock_sku"),
        @Index(name = "idx_stock_receipt_item_ingredient_sku", columnList = "ingredient_sku")
})
public class StockReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long receiptItemId;

    @ManyToOne
    @JoinColumn(name = "receipt_id", nullable = false)
    @JsonIgnore
    private StockReceipt stockReceipt;

    @ManyToOne
    @JoinColumn(name = "item_stock_sku")
    private ItemStock itemStock;

    @ManyToOne
    @JoinColumn(name = "ingredient_sku")
    private IngredientStock ingredientStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "sku_type", nullable = false, length = 30)
    private StockReceiptSkuType skuType;

    @Column(name = "sku_name", nullable = false)
    private String skuName;

    @Column(nullable = false)
    private Double quantityReceived;

    @Column(name = "entered_qty", nullable = false)
    private Double enteredQty;

    @ManyToOne
    @JoinColumn(name = "unit_id")
    private UnitMaster unit;

    @Column(nullable = false)
    private Double unitCost;

    @Column(nullable = false)
    private Double totalCost;
}
