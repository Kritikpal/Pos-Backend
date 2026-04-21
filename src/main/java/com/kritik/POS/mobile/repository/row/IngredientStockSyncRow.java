package com.kritik.POS.mobile.repository.row;

import java.time.LocalDateTime;

public record IngredientStockSyncRow(
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
        Boolean isDeleted,
        LocalDateTime lastRestockedAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements TombstoneSyncRow {

    @Override
    public String cursorKey() {
        return sku;
    }
}
