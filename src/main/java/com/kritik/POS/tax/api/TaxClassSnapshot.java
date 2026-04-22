package com.kritik.POS.tax.api;

public record TaxClassSnapshot(
        Long id,
        Long restaurantId,
        String code,
        boolean exempt
) {
}
