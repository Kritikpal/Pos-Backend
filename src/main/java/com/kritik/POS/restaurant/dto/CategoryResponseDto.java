package com.kritik.POS.restaurant.dto;

import java.time.LocalDateTime;

public record CategoryResponseDto(
        Long categoryId,
        Long restaurantId,
        String categoryName,
        String categoryDescription,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
