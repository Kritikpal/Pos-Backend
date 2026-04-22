package com.kritik.POS.tax.api;

import java.math.BigDecimal;

public record TaxableChargeComponent(
        String referenceKey,
        String taxClassCode,
        Long taxClassId,
        BigDecimal taxableAmount,
        boolean priceIncludesTax
) {
}
