package com.kritik.POS.mobile.dto.response.syncDtos;

import java.time.LocalDateTime;

public record MenuRecipeSyncDto(
        Long recipeId,
        Long menuItemId,
        Long restaurantId,
        Integer batchSize,
        Boolean isActive,
        LocalDateTime syncUpdatedAt
) {
}
