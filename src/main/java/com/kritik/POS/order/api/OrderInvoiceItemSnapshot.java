package com.kritik.POS.order.api;

import java.math.BigDecimal;

public record OrderInvoiceItemSnapshot(
        String lineName,
        Integer amount,
        BigDecimal unitPrice,
        BigDecimal lineTaxAmount,
        BigDecimal lineTotalAmount
) {
}
