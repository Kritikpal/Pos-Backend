package com.kritik.POS.inventory.models.request;

import jakarta.validation.constraints.Size;

public record PreparedStockUpdateRequest(
        @Size(max = 30, message = "Unit code must be 30 characters or less")
        String unitCode,
        Boolean isActive
) {
}
