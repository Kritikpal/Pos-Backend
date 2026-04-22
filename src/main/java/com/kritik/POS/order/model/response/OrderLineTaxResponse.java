package com.kritik.POS.order.model.response;

import com.kritik.POS.order.entity.OrderLineTax;
import com.kritik.POS.tax.api.TaxCalculationMode;
import com.kritik.POS.tax.api.TaxCompoundMode;
import com.kritik.POS.tax.api.TaxValueType;
import java.math.BigDecimal;

public record OrderLineTaxResponse(
        String referenceKey,
        String taxDefinitionCode,
        String taxDisplayName,
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
    public static OrderLineTaxResponse fromEntity(OrderLineTax entity) {
        return new OrderLineTaxResponse(
                entity.getReferenceKey(),
                entity.getTaxDefinitionCode(),
                entity.getTaxDisplayName(),
                entity.getValueType(),
                entity.getRateOrAmount(),
                entity.getCalculationMode(),
                entity.getCompoundMode(),
                entity.getSequenceNo(),
                entity.getTaxableBaseAmount(),
                entity.getTaxAmount(),
                entity.getCurrencyCode(),
                entity.getJurisdictionCountryCode(),
                entity.getJurisdictionRegionCode()
        );
    }
}
