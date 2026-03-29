package com.kritik.POS.restaurant.projection;

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
    Integer getTotalQuantity();
    Double getTotalCost();
    LocalDateTime getCreatedAt();
}
