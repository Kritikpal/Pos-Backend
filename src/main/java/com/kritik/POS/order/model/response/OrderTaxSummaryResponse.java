package com.kritik.POS.order.model.response;

import com.kritik.POS.order.entity.OrderTaxSummary;
import java.math.BigDecimal;

public record OrderTaxSummaryResponse(
        String taxDefinitionCode,
        String taxDisplayName,
        BigDecimal taxableBaseAmount,
        BigDecimal taxAmount,
        String currencyCode
) {
    public static OrderTaxSummaryResponse fromEntity(OrderTaxSummary entity) {
        return new OrderTaxSummaryResponse(
                entity.getTaxDefinitionCode(),
                entity.getTaxDisplayName(),
                entity.getTaxableBaseAmount(),
                entity.getTaxAmount(),
                entity.getCurrencyCode()
        );
    }
}
