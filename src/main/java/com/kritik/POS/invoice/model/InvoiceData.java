package com.kritik.POS.invoice.model;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceData(
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
        List<InvoiceTaxSummary> taxSummaries,
        List<InvoiceItem> items
) {}
