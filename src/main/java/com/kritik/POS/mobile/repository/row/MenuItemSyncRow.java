package com.kritik.POS.mobile.repository.row;

import com.kritik.POS.restaurant.entity.enums.MenuType;
import java.time.LocalDateTime;

public record MenuItemSyncRow(
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
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements TombstoneSyncRow {

    @Override
    public String cursorKey() {
        return String.valueOf(menuItemId);
    }
}
