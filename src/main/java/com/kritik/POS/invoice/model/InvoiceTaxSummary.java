package com.kritik.POS.invoice.model;

import java.math.BigDecimal;

public record InvoiceTaxSummary(
        String taxDisplayName,
        BigDecimal taxableBaseAmount,
        BigDecimal taxAmount,
        String currencyCode
) {
}
