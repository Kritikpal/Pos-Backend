package com.kritik.POS.inventory.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RecipeManagementRequest(
        @NotNull(message = "Menu item id is required")
        Long menuItemId,
        @NotNull(message = "Recipe batch size is required")
        @Positive(message = "Recipe batch size must be greater than 0")
        Integer batchSize,
        Boolean active,
        @NotEmpty(message = "Recipe ingredients are required")
        List<@Valid IngredientUsageRequest> ingredients
) {
    public record IngredientUsageRequest(
            @NotBlank(message = "Ingredient sku is required")
            String ingredientSku,
            @NotNull(message = "Ingredient quantity is required")
            @DecimalMin(value = "0.0001", inclusive = true, message = "Ingredient quantity must be greater than 0")
            Double quantityRequired
    ) {
    }
}
