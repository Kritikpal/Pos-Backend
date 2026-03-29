package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.projection.StockReceiptSummaryProjection;

import java.time.LocalDateTime;

public record StockReceiptResponseDto(
        Long receiptId,
        String receiptNumber,
        Long restaurantId,
        Long supplierId,
        String supplierName,
        String invoiceNumber,
        LocalDateTime receivedAt,
        Integer totalItems,
        Integer totalQuantity,
        Double totalCost,
        LocalDateTime createdAt
) {
    public static StockReceiptResponseDto toStockReceiptDto(StockReceiptSummaryProjection projection) {
        return new StockReceiptResponseDto(
                projection.getReceiptId(),
                projection.getReceiptNumber(),
                projection.getRestaurantId(),
                projection.getSupplierId(),
                projection.getSupplierName(),
                projection.getInvoiceNumber(),
                projection.getReceivedAt(),
                projection.getTotalItems(),
                projection.getTotalQuantity(),
                projection.getTotalCost(),
                projection.getCreatedAt()
        );
    }

}
