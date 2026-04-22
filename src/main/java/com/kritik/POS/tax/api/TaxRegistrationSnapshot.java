package com.kritik.POS.tax.api;

public record TaxRegistrationSnapshot(
        Long id,
        String registrationNumber,
        String countryCode,
        String regionCode
) {
}
