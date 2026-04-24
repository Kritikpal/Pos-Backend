package com.kritik.POS.inventory.models.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UnitConversionRequest(
        @NotNull(message = "Unit id is required")
        Long unitId,
        @NotNull(message = "Factor to base is required")
        @DecimalMin(value = "0.000001", message = "Factor to base must be greater than 0")
        BigDecimal factorToBase,
        Boolean purchaseAllowed,
        Boolean saleAllowed,
        Boolean active
) {
}
