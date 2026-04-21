package com.kritik.POS.mobile.dto.response.syncDtos;

import java.time.LocalDateTime;

public record PreparedStockSyncDto(
        Long menuItemId,
        Long restaurantId,
        Double availableQty,
        Double reservedQty,
        String unitCode,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
