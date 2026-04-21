package com.kritik.POS.mobile.repository.row;

import com.kritik.POS.tax.entity.enums.TaxCalculationMode;
import com.kritik.POS.tax.entity.enums.TaxCompoundMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaxRuleSyncRow(
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
        Boolean isActive,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements TombstoneSyncRow {

    @Override
    public String cursorKey() {
        return String.valueOf(id);
    }
}
