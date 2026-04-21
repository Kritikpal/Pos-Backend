package com.kritik.POS.mobile.dto.response.syncDtos;

import java.time.LocalDateTime;

public record CategorySyncDto(
        Long categoryId,
        Long restaurantId,
        String categoryName,
        String categoryDescription,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
