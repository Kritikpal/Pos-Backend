package com.kritik.POS.inventory.models.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ProductionEntryCreateRequest(
        Long restaurantId,
        @NotNull(message = "Menu item id is required")
        Long menuItemId,
        @NotNull(message = "Produced quantity is required")
        @DecimalMin(value = "0.0001", inclusive = true, message = "Produced quantity must be greater than 0")
        Double producedQty,
        @NotBlank(message = "Unit code is required")
        String unitCode,
        LocalDateTime productionTime,
        String notes
) {
}
