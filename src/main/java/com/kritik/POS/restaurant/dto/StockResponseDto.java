package com.kritik.POS.restaurant.dto;

import java.time.LocalDateTime;

public record StockResponseDto(
        String sku,
        Long restaurantId,
        Long menuItemId,
        String itemName,
        Long categoryId,
        String categoryName,
        Integer totalStock,
        Integer reorderLevel,
        String unitOfMeasure,
        Boolean lowStock,
        Long supplierId,
        String supplierName,
        Boolean isActive,
        Boolean isAvailable,
        LocalDateTime lastRestockedAt,
        LocalDateTime updatedAt
) {
}
