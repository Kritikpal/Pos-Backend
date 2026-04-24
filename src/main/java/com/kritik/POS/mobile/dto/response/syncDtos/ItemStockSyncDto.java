package com.kritik.POS.mobile.dto.response.syncDtos;

import java.time.LocalDateTime;

public record ItemStockSyncDto(
        String sku,
        Long restaurantId,
        Long menuItemId,
        Long supplierId,
        String supplierName,
        Integer totalStock,
        Integer reorderLevel,
        String unitOfMeasure,
        Long baseUnitId,
        String baseUnitCode,
        Boolean isActive,
        LocalDateTime lastRestockedAt,
        LocalDateTime updatedAt
) {
}
