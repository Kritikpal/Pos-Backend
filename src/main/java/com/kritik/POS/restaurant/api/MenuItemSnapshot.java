package com.kritik.POS.restaurant.api;

import java.util.List;

public record MenuItemSnapshot(
        Long id,
        Long restaurantId,
        Long taxClassId,
        String itemName,
        String description,
        boolean active,
        boolean deleted,
        boolean available,
        MenuItemType menuType,
        MenuPriceSnapshot price,
        String directStockSku,
        List<IngredientUsageSnapshot> ingredientUsages
) {
}
