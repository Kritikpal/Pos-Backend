package com.kritik.POS.mobile.repository.row;

import java.time.LocalDateTime;

public record MenuRecipeItemSyncRow(
        Long recipeItemId,
        Long recipeId,
        Long menuItemId,
        Long restaurantId,
        String ingredientSku,
        Double quantityRequired,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements SyncStreamRow {

    @Override
    public String cursorKey() {
        return String.valueOf(recipeItemId);
    }
}
