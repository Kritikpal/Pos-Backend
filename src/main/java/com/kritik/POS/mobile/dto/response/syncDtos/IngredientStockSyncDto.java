package com.kritik.POS.mobile.dto.response.syncDtos;

import java.time.LocalDateTime;

public record IngredientStockSyncDto(
        String sku,
        Long restaurantId,
        String ingredientName,
        String description,
        Long supplierId,
        String supplierName,
        Double totalStock,
        Double reorderLevel,
        String unitOfMeasure,
        Boolean isActive,
        LocalDateTime lastRestockedAt,
        LocalDateTime updatedAt
) {
}
