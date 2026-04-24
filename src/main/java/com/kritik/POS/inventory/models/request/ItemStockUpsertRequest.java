package com.kritik.POS.inventory.models.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ItemStockUpsertRequest(
        @NotNull(message = "Menu item id is required")
        Long menuItemId,
        @PositiveOrZero(message = "Reorder level must be 0 or greater")
        Integer reorderLevel,
        Long baseUnitId,
        @Size(max = 30, message = "Unit of measure must be 30 characters or less")
        String unitOfMeasure,
        List<UnitConversionRequest> conversions,
        Long supplierId,
        Boolean isActive
) {
}
