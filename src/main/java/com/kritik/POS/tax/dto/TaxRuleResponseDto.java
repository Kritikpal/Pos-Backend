package com.kritik.POS.tax.dto;

import com.kritik.POS.tax.entity.enums.TaxCalculationMode;
import com.kritik.POS.tax.entity.enums.TaxCompoundMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaxRuleResponseDto(
        Long id,
        Long restaurantId,
        Long taxDefinitionId,
        Long taxClassId,
        TaxCalculationMode calculationMode,
        TaxCompoundMode compoundMode,
        Integer sequenceNo,
        LocalDate validFrom,
        LocalDate validTo,
        String countryCode,
        String regionCode,
        String buyerTaxCategory,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer priority,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
