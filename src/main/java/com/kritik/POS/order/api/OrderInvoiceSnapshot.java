package com.kritik.POS.order.api;

import java.math.BigDecimal;
import java.util.List;

public record OrderInvoiceSnapshot(
        Long orderDbId,
        String orderId,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal feeAmount,
        BigDecimal grandTotal,
        String sellerRegistrationNumber,
        String buyerName,
        String buyerTaxId,
        String buyerTaxCategory,
        List<OrderInvoiceTaxSummarySnapshot> taxSummaries,
        List<OrderInvoiceItemSnapshot> items
) {
}
