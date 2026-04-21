package com.kritik.POS.configuredmenu.models.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ConfiguredMenuTemplateRequest(
        @NotNull(message = "Parent menu item id is required")
        Long parentMenuItemId,
        Boolean isActive,
        @NotEmpty(message = "At least one slot is required")
        List<@Valid ConfiguredMenuSlotRequest> slots
) {
    public record ConfiguredMenuSlotRequest(
            @NotBlank(message = "Slot key is required")
            String slotKey,
            @NotBlank(message = "Slot name is required")
            String slotName,
            @NotNull(message = "Minimum selections is required")
            @Min(value = 0, message = "Minimum selections must be 0 or greater")
            Integer minSelections,
            @NotNull(message = "Maximum selections is required")
            @Min(value = 1, message = "Maximum selections must be at least 1")
            Integer maxSelections,
            @NotNull(message = "Display order is required")
            @Min(value = 0, message = "Display order must be 0 or greater")
            Integer displayOrder,
            Boolean isRequired,
            @NotEmpty(message = "At least one option is required")
            List<@Valid ConfiguredMenuOptionRequest> options
    ) {
    }

    public record ConfiguredMenuOptionRequest(
            @NotNull(message = "Child menu item id is required")
            Long childMenuItemId,
            @NotNull(message = "Price delta is required")
            @DecimalMin(value = "0.0", inclusive = true, message = "Price delta must be 0 or greater")
            BigDecimal priceDelta,
            @NotNull(message = "Display order is required")
            @Min(value = 0, message = "Display order must be 0 or greater")
            Integer displayOrder,
            Boolean isDefault,
            @Min(value = 0, message = "Minimum quantity must be 0 or greater")
            Integer minQuantity
    ) {
    }
}
