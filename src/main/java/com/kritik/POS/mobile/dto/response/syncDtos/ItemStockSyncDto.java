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
        Boolean isActive,
        LocalDateTime lastRestockedAt,
        LocalDateTime updatedAt
) {
}
