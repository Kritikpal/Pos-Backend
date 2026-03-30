package com.kritik.POS.inventory.models.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ItemStockUpsertRequest(
        @NotNull(message = "Menu item id is required")
        Long menuItemId,
        @NotNull(message = "Total stock is required")
        @PositiveOrZero(message = "Total stock must be 0 or greater")
        Integer totalStock,
        @PositiveOrZero(message = "Reorder level must be 0 or greater")
        Integer reorderLevel,
        @Size(max = 30, message = "Unit of measure must be 30 characters or less")
        String unitOfMeasure,
        Long supplierId,
        Boolean isActive
) {
}
