package com.kritik.POS.tax.api;

import java.math.BigDecimal;
import java.util.List;

public record TaxComputationResult(
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal feeAmount,
        BigDecimal grandTotal,
        List<AppliedTaxComponent> appliedTaxes
) {
}
