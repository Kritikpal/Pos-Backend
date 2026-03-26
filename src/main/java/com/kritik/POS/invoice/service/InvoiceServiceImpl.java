package com.kritik.POS.invoice.service;

import com.kritik.POS.common.service.FileUploadService;
import com.kritik.POS.invoice.entity.Invoice;
import com.kritik.POS.invoice.model.FileDownloadResponse;
import com.kritik.POS.invoice.model.InvoiceData;
import com.kritik.POS.invoice.model.InvoiceInfo;
import com.kritik.POS.invoice.model.InvoiceItem;
import com.kritik.POS.invoice.repository.InvoiceRepository;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.restaurant.entity.ProductFile;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TemplateEngine templateEngine;
    private final FileUploadService fileUploadService;

    @Override
    public void generateInvoice(Order order) {

        if (invoiceRepository.existsByOrder(order)) {
            return;
        }

        String invoiceNumber = "INV-" + System.currentTimeMillis();

        // 🔥 DTO mapping
        InvoiceData invoiceData = mapToInvoiceData(order);

        // 1. Prepare HTML using Thymeleaf
        Context context = new Context();
        context.setVariable("invoice", invoiceData);

        String html = templateEngine.process("invoice-template", context);

        // 2. Generate PDF
        String filePath = generatePdf(invoiceNumber, html);

        // 3. Save entity
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setOrder(order);
        invoice.setTotalAmount(order.getTotalPrice());
        invoice.setFilePath(filePath);
        invoice.setGeneratedAt(LocalDateTime.now());

        invoiceRepository.save(invoice);
    }

    private InvoiceData mapToInvoiceData(Order order) {

        List<InvoiceItem> items = new ArrayList<>();
        for (SaleItem item : order.getOrderItemList()) {
            InvoiceItem invoiceItem = new InvoiceItem(
                    item.getSaleItemName(),
                    item.getAmount(),
                    item.getSaleItemPrice()
            );
            items.add(invoiceItem);
        }

        return new InvoiceData(
                order.getOrderId(),
                order.getTotalPrice(),
                items
        );
    }

    @Override
    public List<InvoiceInfo> getInvoices(int page, int size, String invoiceNumber) {

        Pageable pageable = PageRequest.of(page, size);

        String search = (invoiceNumber == null || invoiceNumber.isBlank())
                ? null
                : invoiceNumber;

        return invoiceRepository.searchInvoices(search, pageable).getContent();
    }

    @Override
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    @Override
    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    @Override
    public FileDownloadResponse downloadInvoice(String invoiceNumber) {

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        try {
            Path path = Paths.get(invoice.getFilePath());

            if (!Files.exists(path)) {
                throw new RuntimeException("Invoice file not found");
            }

            Resource resource = new UrlResource(path.toUri());

            String fileName = invoice.getInvoiceNumber() + ".pdf";

            return new FileDownloadResponse(resource, fileName);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Error while reading file", e);
        }
    }


    private String generatePdf(String invoiceNumber, String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            // ✅ Generate PDF in memory
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();

            byte[] pdfBytes = os.toByteArray();

            // ✅ Delegate storage to FileUploadService
            ProductFile file = fileUploadService.uploadFile(
                    pdfBytes,
                    invoiceNumber + ".pdf",
                    "application/pdf"
            );

            // ✅ Return stored path (or URL)
            return file.getUrl();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }
}