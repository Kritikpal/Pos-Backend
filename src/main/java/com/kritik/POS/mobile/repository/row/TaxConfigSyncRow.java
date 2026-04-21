package com.kritik.POS.mobile.repository.row;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxConfigSyncRow(
        Long taxId,
        Long restaurantId,
        String taxName,
        BigDecimal taxAmount,
        Boolean isActive,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements TombstoneSyncRow {

    @Override
    public String cursorKey() {
        return String.valueOf(taxId);
    }
}
