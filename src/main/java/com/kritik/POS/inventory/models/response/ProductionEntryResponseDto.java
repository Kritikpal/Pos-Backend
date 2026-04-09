package com.kritik.POS.inventory.models.response;

import java.time.LocalDateTime;
import java.util.List;

public record ProductionEntryResponseDto(
        Long id,
        Long restaurantId,
        Long menuItemId,
        String menuItemName,
        Double producedQty,
        String unitCode,
        LocalDateTime productionTime,
        String notes,
        Long createdBy,
        LocalDateTime createdAt,
        Double availablePreparedQty,
        List<ProductionEntryItemResponseDto> ingredients
) {
}
