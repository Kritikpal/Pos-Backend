package com.kritik.POS.tax.dto;

import java.util.List;

public record TaxCatalogSeedResponse(
        Long restaurantId,
        List<TaxCatalogSeedCountryResult> datasets
) {
}
