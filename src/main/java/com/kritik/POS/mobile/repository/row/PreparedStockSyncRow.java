package com.kritik.POS.mobile.repository.row;

import java.time.LocalDateTime;

public record PreparedStockSyncRow(
        Long menuItemId,
        Long restaurantId,
        Double availableQty,
        Double reservedQty,
        String unitCode,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements SyncStreamRow {

    @Override
    public String cursorKey() {
        return String.valueOf(menuItemId);
    }
}
