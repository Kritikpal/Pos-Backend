package com.kritik.POS.tax.dto;

import com.kritik.POS.tax.entity.enums.TaxCalculationMode;
import com.kritik.POS.tax.entity.enums.TaxCompoundMode;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxRuleRequest(
        Long id,
        @NotNull(message = "restaurantId is required") Long restaurantId,
        @NotNull(message = "taxDefinitionId is required") Long taxDefinitionId,
        @NotNull(message = "taxClassId is required") Long taxClassId,
        @NotNull(message = "calculationMode is required") TaxCalculationMode calculationMode,
        @NotNull(message = "compoundMode is required") TaxCompoundMode compoundMode,
        @NotNull(message = "sequenceNo is required") Integer sequenceNo,
        LocalDate validFrom,
        LocalDate validTo,
        String countryCode,
        String regionCode,
        String buyerTaxCategory,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer priority,
        Boolean isActive
) {
}
