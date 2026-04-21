package com.kritik.POS.tax.model;

import com.kritik.POS.tax.entity.enums.TaxCalculationMode;
import com.kritik.POS.tax.entity.enums.TaxCompoundMode;
import com.kritik.POS.tax.entity.enums.TaxDefinitionKind;
import com.kritik.POS.tax.entity.enums.TaxValueType;
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
