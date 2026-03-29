package com.kritik.POS.restaurant.dto;

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
}
