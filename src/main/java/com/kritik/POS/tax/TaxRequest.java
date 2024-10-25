package com.kritik.POS.tax;


import jakarta.validation.constraints.NotNull;

public record TaxRequest(
        Long taxId,
        @NotNull(message = "Please Provide A tax name") String taxName,
        @NotNull(message = "Tax amount is required") Double taxAmount,
        @NotNull(message = "Please provide is active flag") boolean active) {
}
