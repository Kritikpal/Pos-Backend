package com.kritik.POS.tax.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TaxRegistrationRequest(
        Long id,
        @NotNull(message = "restaurantId is required") Long restaurantId,
        @NotBlank(message = "schemeCode is required") String schemeCode,
        @NotBlank(message = "registrationNumber is required") String registrationNumber,
        @NotBlank(message = "legalName is required") String legalName,
        String countryCode,
        String regionCode,
        String placeOfBusiness,
        Boolean isDefault,
        LocalDate validFrom,
        LocalDate validTo,
        Boolean isActive
) {
}
