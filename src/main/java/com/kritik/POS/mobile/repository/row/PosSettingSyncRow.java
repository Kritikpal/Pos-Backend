package com.kritik.POS.mobile.repository.row;

import java.time.LocalDateTime;

public record PosSettingSyncRow(
        Long restaurantId,
        Long chainId,
        String restaurantCode,
        String restaurantName,
        String currency,
        String timezone,
        String gstNumber,
        String phoneNumber,
        String email,
        LocalDateTime updatedAt
) implements SyncStreamRow {

    @Override
    public LocalDateTime syncTs() {
        return updatedAt;
    }

    @Override
    public String cursorKey() {
        return String.valueOf(restaurantId);
    }
}
