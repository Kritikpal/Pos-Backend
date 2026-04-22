package com.kritik.POS.order.api;

import java.math.BigDecimal;

public record OrderInvoiceTaxSummarySnapshot(
        String taxDisplayName,
        BigDecimal taxableBaseAmount,
        BigDecimal taxAmount,
        String currencyCode
) {
}
