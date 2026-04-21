package com.kritik.POS.mobile.dto.response.syncDtos;

import com.kritik.POS.restaurant.entity.enums.MenuType;
import java.time.LocalDateTime;

public record MenuItemSyncDto(
        Long menuItemId,
        Long restaurantId,
        Long categoryId,
        Long priceId,
        Long taxClassId,
        String productImageUrl,
        String itemName,
        String description,
        Boolean isAvailable,
        Boolean isActive,
        Boolean isTrending,
        MenuType menuType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
