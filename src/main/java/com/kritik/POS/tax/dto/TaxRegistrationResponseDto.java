package com.kritik.POS.tax.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaxRegistrationResponseDto(
        Long id,
        Long restaurantId,
        String schemeCode,
        String registrationNumber,
        String legalName,
        String countryCode,
        String regionCode,
        String placeOfBusiness,
        boolean isDefault,
        LocalDate validFrom,
        LocalDate validTo,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
