package com.kritik.POS.inventory.projection;

import java.time.LocalDateTime;

public interface StockReceiptSummaryProjection {
    Long getReceiptId();
    String getReceiptNumber();
    Long getRestaurantId();
    Long getSupplierId();
    String getSupplierName();
    String getInvoiceNumber();
    LocalDateTime getReceivedAt();
    Integer getTotalItems();
    Double getTotalQuantity();
    Double getTotalCost();
    LocalDateTime getCreatedAt();
}
