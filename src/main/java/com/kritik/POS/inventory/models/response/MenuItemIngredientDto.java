package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.projection.MenuItemIngredientProjection;

import java.time.LocalDateTime;

public record MenuItemIngredientDto(
        Long id,
        Long menuId,
        Long restaurantId,
        String menuItemName,
        String ingredientSku,
        String ingredientName,
        Double quantityRequired,
        Integer batchSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MenuItemIngredientDto fromProjection(MenuItemIngredientProjection p) {
        return new MenuItemIngredientDto(
                p.getId(),
                p.getMenuId(),
                p.getRestaurantId(),
                p.getMenuItemName(),
                p.getIngredientSku(),
                p.getIngredientName(),
                p.getQuantityRequired(),
                p.getBatchSize(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
