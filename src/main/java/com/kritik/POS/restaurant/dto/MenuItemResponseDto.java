package com.kritik.POS.restaurant.dto;

import com.kritik.POS.restaurant.entity.enums.MenuType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MenuItemResponseDto(
        Long id,
        Long restaurantId,
        String sku,
        String productImage,
        String itemName,
        MenuType menuType,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        Boolean isAvailable,
        Boolean isActive,
        Integer totalStock,
        String unitOfMeasure,
        LocalDateTime updatedAt
) {
}
