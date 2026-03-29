package com.kritik.POS.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "stock_receipt_item", indexes = {
        @Index(name = "idx_stock_receipt_item_receipt", columnList = "receipt_id"),
        @Index(name = "idx_stock_receipt_item_sku", columnList = "item_stock_sku")
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
    @JoinColumn(name = "item_stock_sku", nullable = false)
    private ItemStock itemStock;

    @Column(name = "menu_item_name", nullable = false)
    private String menuItemName;

    @Column(nullable = false)
    private Integer quantityReceived;

    @Column(nullable = false)
    private Double unitCost;

    @Column(nullable = false)
    private Double totalCost;
}
