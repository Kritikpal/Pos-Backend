package com.kritik.POS.invoice.model;

import com.kritik.POS.invoice.entity.Invoice;

import java.time.LocalDateTime;

/**
 * Projection for {@link Invoice}
 */
public interface InvoiceInfo {
    String getInvoiceNumber();

    Double getTotalAmount();

    String getFilePath();

    LocalDateTime getGeneratedAt();
}