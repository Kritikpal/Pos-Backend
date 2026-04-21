package com.kritik.POS.mobile.repository.row;

import java.time.LocalDateTime;

public record ItemStockSyncRow(
        String sku,
        Long restaurantId,
        Long menuItemId,
        Long supplierId,
        String supplierName,
        Integer totalStock,
        Integer reorderLevel,
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
