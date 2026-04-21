package com.kritik.POS.mobile.repository.row;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaxRegistrationSyncRow(
        Long id,
        Long restaurantId,
        String schemeCode,
        String registrationNumber,
        String legalName,
        String countryCode,
        String regionCode,
        String placeOfBusiness,
        Boolean isDefault,
        LocalDate validFrom,
        LocalDate validTo,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements SyncStreamRow {

    @Override
    public String cursorKey() {
        return String.valueOf(id);
    }
}
