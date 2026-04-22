package com.kritik.POS.inventory.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

@Validated
public record IngredientRequest(
        String sku,
        Long restaurantId,
        @NotBlank(message = "Ingredient name is required")
        String ingredientName,
        String description,
        @Size(max = 120, message = "Category must be 120 characters or less")
        String category,
        Long supplierId,
        @PositiveOrZero(message = "Reorder level must be 0 or greater")
        Double reorderLevel,
        @Size(max = 30, message = "Unit of measure must be 30 characters or less")
        String unitOfMeasure,
        Boolean isActive
) {
}
