package com.kritik.POS.order.model.request;

public record OrderTaxContextRequest(
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
