package com.kritik.POS.mobile.dto.response.syncDtos;

import java.time.LocalDateTime;

public record MenuRecipeItemSyncDto(
        Long recipeItemId,
        Long recipeId,
        Long menuItemId,
        Long restaurantId,
        String ingredientSku,
        Double quantityRequired,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
