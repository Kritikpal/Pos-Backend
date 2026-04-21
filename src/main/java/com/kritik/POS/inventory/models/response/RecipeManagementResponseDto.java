package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;

import java.time.Instant;
import java.util.List;

public record RecipeManagementResponseDto(
        Long id,
        Long menuItemId,
        Long restaurantId,
        String menuItemName,
        Integer batchSize,
        Boolean active,
        Instant createdDate,
        Instant lastModifiedDate,
        List<IngredientUsageDto> ingredients
) {
    public record IngredientUsageDto(
            Long id,
            String ingredientSku,
            String ingredientName,
            Double quantityRequired,
            String unitOfMeasure,
            Double availableStock
    ) {
        public static IngredientUsageDto fromEntity(MenuItemIngredient ingredientUsage) {
            return new IngredientUsageDto(
                    ingredientUsage.getId(),
                    ingredientUsage.getIngredientStock().getSku(),
                    ingredientUsage.getIngredientStock().getIngredientName(),
                    ingredientUsage.getQuantityRequired(),
                    ingredientUsage.getIngredientStock().getUnitOfMeasure(),
                    ingredientUsage.getIngredientStock().getTotalStock()
            );
        }
    }

    public static RecipeManagementResponseDto fromEntity(MenuRecipe recipe) {
        return new RecipeManagementResponseDto(
                recipe.getId(),
                recipe.getMenuItem().getId(),
                recipe.getMenuItem().getRestaurantId(),
                recipe.getMenuItem().getItemName(),
                recipe.getBatchSize(),
                recipe.getActive(),
                recipe.getCreatedDate(),
                recipe.getLastModifiedDate(),
                recipe.getIngredientUsages().stream()
                        .map(IngredientUsageDto::fromEntity)
                        .toList()
        );
    }
}
