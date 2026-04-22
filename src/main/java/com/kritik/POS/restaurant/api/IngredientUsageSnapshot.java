package com.kritik.POS.restaurant.api;

public record IngredientUsageSnapshot(
        String ingredientSku,
        Double quantityRequired,
        Integer batchSize
) {
}
