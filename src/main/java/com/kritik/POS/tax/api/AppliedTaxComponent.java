package com.kritik.POS.tax.api;

import java.math.BigDecimal;

public record AppliedTaxComponent(
        String referenceKey,
        String taxDefinitionCode,
        String taxDisplayName,
        TaxDefinitionKind kind,
        TaxValueType valueType,
        BigDecimal rateOrAmount,
        TaxCalculationMode calculationMode,
        TaxCompoundMode compoundMode,
        Integer sequenceNo,
        BigDecimal taxableBaseAmount,
        BigDecimal taxAmount,
        String currencyCode,
        String jurisdictionCountryCode,
        String jurisdictionRegionCode
) {
}
