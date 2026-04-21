package com.kritik.POS.tax.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaxClassRequest(
        Long id,
        @NotNull(message = "restaurantId is required") Long restaurantId,
        @NotBlank(message = "code is required") String code,
        @NotBlank(message = "name is required") String name,
        String description,
        Boolean isExempt,
        Boolean isActive
) {
}
