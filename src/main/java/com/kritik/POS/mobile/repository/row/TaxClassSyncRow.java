package com.kritik.POS.mobile.repository.row;

import java.time.LocalDateTime;

public record TaxClassSyncRow(
        Long id,
        Long restaurantId,
        String code,
        String name,
        String description,
        Boolean isExempt,
        Boolean isActive,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements TombstoneSyncRow {

    @Override
    public String cursorKey() {
        return String.valueOf(id);
    }
}
