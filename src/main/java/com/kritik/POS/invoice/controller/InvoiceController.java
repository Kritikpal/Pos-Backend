package com.kritik.POS.invoice.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.invoice.entity.Invoice;
import com.kritik.POS.invoice.model.FileDownloadResponse;
import com.kritik.POS.invoice.model.InvoiceInfo;
import com.kritik.POS.invoice.service.InvoiceService;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/{invoiceNumber}/download")
    public ResponseEntity<Resource> downloadInvoice(@PathVariable String invoiceNumber) {
        FileDownloadResponse response = invoiceService.downloadInvoice(invoiceNumber);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + response.fileName() + "\"")
                .body(response.resource());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceInfo>>> getInvoices(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be at least 1") int size,
            @RequestParam(required = false) String invoiceNumber
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(invoiceService.getInvoices(page, size, invoiceNumber)));
    }

    @GetMapping("/{invoiceNumber}")
    public ResponseEntity<ApiResponse<Invoice>> getInvoice(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(invoiceService.getInvoiceByNumber(invoiceNumber)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok(ApiResponse.SUCCESS(Boolean.TRUE, "Invoice deleted successfully"));
    }
}
