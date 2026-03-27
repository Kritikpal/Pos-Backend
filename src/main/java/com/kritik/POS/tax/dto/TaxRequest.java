package com.kritik.POS.tax.dto;


import jakarta.validation.constraints.NotNull;

public record TaxRequest(
        Long taxId,
        Long restaurantId,
        @NotNull(message = "Please Provide A tax name") String taxName,
        @NotNull(message = "Tax amount is required") Double taxAmount,
        @NotNull(message = "Please provide is active flag") boolean active) {
}
