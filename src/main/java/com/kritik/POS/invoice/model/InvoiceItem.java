package com.kritik.POS.invoice.model;

public record InvoiceItem(
        String name,
        Integer quantity,
        Double price
) {}