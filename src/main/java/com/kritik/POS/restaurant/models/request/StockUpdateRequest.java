package com.kritik.POS.restaurant.models.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record StockUpdateRequest(
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
