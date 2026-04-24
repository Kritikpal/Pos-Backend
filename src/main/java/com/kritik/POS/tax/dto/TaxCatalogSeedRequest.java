package com.kritik.POS.tax.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TaxCatalogSeedRequest(
        @NotNull(message = "restaurantId is required") Long restaurantId,
        @NotEmpty(message = "countries is required") List<@NotNull(message = "country code is required") String> countries,
        LocalDate effectiveFrom,
        Boolean overwriteExisting
) {
}
