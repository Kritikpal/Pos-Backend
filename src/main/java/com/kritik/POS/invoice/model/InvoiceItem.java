package com.kritik.POS.invoice.model;

import java.math.BigDecimal;

public record InvoiceItem(
        String name,
        Integer quantity,
        BigDecimal price,
        BigDecimal taxAmount,
        BigDecimal total
) {
}
