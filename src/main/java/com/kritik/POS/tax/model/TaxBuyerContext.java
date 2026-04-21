package com.kritik.POS.tax.model;

public record TaxBuyerContext(
        String buyerName,
        String buyerTaxId,
        String buyerTaxCategory,
        String buyerCountryCode,
        String buyerRegionCode,
        String billingAddressJson,
        String placeOfSupplyCountryCode,
        String placeOfSupplyRegionCode
) {
}
