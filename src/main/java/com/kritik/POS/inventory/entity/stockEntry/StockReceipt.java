package com.kritik.POS.inventory.entity.stockEntry;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "stock_receipt", indexes = {
        @Index(name = "idx_stock_receipt_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_stock_receipt_number", columnList = "receipt_number", unique = true)
})
public class StockReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long receiptId;

    @Column(name = "receipt_number", nullable = false, unique = true)
    private String receiptNumber;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false)
    private Integer totalItems = 0;

    @Column(nullable = false)
    private Double totalQuantity = 0.0;

    @Column(nullable = false)
    private Double totalCost = 0.0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "stockReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockReceiptItem> receiptItems = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
