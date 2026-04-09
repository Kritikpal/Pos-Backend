package com.kritik.POS.inventory.models.response;

public record ProductionEntryItemResponseDto(
        String ingredientSku,
        String ingredientName,
        Double deductedQty,
        String unitCode
) {
}
