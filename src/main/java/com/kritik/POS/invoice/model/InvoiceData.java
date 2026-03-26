package com.kritik.POS.invoice.model;

import java.util.List;

public record InvoiceData(
        String orderId,
        Double totalPrice,
        List<InvoiceItem> items
) {}