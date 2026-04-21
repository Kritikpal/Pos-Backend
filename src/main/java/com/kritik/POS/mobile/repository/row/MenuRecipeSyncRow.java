package com.kritik.POS.mobile.repository.row;

import java.time.LocalDateTime;

public record MenuRecipeSyncRow(
        Long recipeId,
        Long menuItemId,
        Long restaurantId,
        Integer batchSize,
        Boolean isActive,
        LocalDateTime syncUpdatedAt
) implements SyncStreamRow {

    @Override
    public LocalDateTime syncTs() {
        return syncUpdatedAt;
    }

    @Override
    public String cursorKey() {
        return String.valueOf(recipeId);
    }
}
