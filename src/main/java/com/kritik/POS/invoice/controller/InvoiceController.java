package com.kritik.POS.invoice.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.invoice.entity.Invoice;
import com.kritik.POS.invoice.model.InvoiceInfo;
import com.kritik.POS.invoice.service.InvoiceService;
import com.kritik.POS.invoice.model.FileDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/{invoiceNumber}/download")
    public ResponseEntity<Resource> downloadInvoice(
            @PathVariable String invoiceNumber
    ) {

        FileDownloadResponse response = invoiceService.downloadInvoice(invoiceNumber);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + response.fileName() + "\"")
                .body(response.resource());
    }

    // 🔹 GET LIST + SEARCH
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceInfo>>> getInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String invoiceNumber
    ) {
        return ResponseEntity.ok(
                ApiResponse.SUCCESS(invoiceService.getInvoices(page, size, invoiceNumber))
        );
    }

    // 🔹 GET SINGLE
    @GetMapping("/{invoiceNumber}")
    public ResponseEntity<Invoice> getInvoice(
            @PathVariable String invoiceNumber
    ) {
        return ResponseEntity.ok(
                invoiceService.getInvoiceByNumber(invoiceNumber)
        );
    }

    // 🔹 DELETE (optional)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}