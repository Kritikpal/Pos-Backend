package com.kritik.POS.order.service.internal;

import java.math.BigDecimal;

public record OrderLinePricingInput(
        String lineKey,
        Long taxClassId,
        String taxClassCode,
        BigDecimal unitListAmount,
        BigDecimal unitDiscountAmount,
        BigDecimal lineSubtotalAmount,
        BigDecimal lineDiscountAmount,
        boolean priceIncludesTax
) {
}
