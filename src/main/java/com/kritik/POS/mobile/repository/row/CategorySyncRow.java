package com.kritik.POS.mobile.repository.row;

import java.time.LocalDateTime;

public record CategorySyncRow(
        Long categoryId,
        Long restaurantId,
        String categoryName,
        String categoryDescription,
        Boolean isActive,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements TombstoneSyncRow {

    @Override
    public String cursorKey() {
        return String.valueOf(categoryId);
    }
}
