package com.kritik.POS.mobile.repository.row;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MenuPriceSyncRow(
        Long priceId,
        Long menuItemId,
        Long restaurantId,
        BigDecimal price,
        BigDecimal discount,
        Boolean priceIncludesTax,
        LocalDateTime syncUpdatedAt
) implements SyncStreamRow {

    @Override
    public LocalDateTime syncTs() {
        return syncUpdatedAt;
    }

    @Override
    public String cursorKey() {
        return String.valueOf(priceId);
    }
}
