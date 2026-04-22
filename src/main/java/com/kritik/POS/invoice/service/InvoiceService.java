package com.kritik.POS.invoice.service;

import com.kritik.POS.invoice.entity.Invoice;
import com.kritik.POS.invoice.model.FileDownloadResponse;
import com.kritik.POS.invoice.model.InvoiceInfo;
import com.kritik.POS.order.api.OrderInvoiceSnapshot;

import java.util.List;

public interface InvoiceService {
    void generateInvoice(OrderInvoiceSnapshot order);

    List<InvoiceInfo> getInvoices(int page, int size, String invoiceNumber);

    Invoice getInvoiceByNumber(String invoiceNumber);

    void deleteInvoice(Long id);

    FileDownloadResponse downloadInvoice(String invoiceNumber);
}
