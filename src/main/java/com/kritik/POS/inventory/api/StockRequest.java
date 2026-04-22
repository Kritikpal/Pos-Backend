package com.kritik.POS.inventory.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StockRequest(
        @NotBlank(message = "SKU is required")
        String sku,
        @Min(value = 1, message = "Amount must be at least 1")
        Integer amount
) {
}
