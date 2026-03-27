package com.kritik.POS.invoice.service;

import com.kritik.POS.common.service.FileUploadService;
import com.kritik.POS.exception.errors.AppException;
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
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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
        InvoiceData invoiceData = mapToInvoiceData(order);

        Context context = new Context();
        context.setVariable("invoice", invoiceData);
        String html = templateEngine.process("invoice-template", context);

        String filePath = generatePdf(invoiceNumber, html);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setOrder(order);
        invoice.setTotalAmount(order.getTotalPrice());
        invoice.setFilePath(filePath);
        invoice.setGeneratedAt(LocalDateTime.now());

        invoiceRepository.save(invoice);
    }

    @Override
    public List<InvoiceInfo> getInvoices(int page, int size, String invoiceNumber) {
        Pageable pageable = PageRequest.of(page, size);
        String search = (invoiceNumber == null || invoiceNumber.isBlank()) ? null : invoiceNumber.trim();
        return invoiceRepository.searchInvoices(search, pageable).getContent();
    }

    @Override
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new AppException("Invoice not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public void deleteInvoice(Long id) {
        if (!invoiceRepository.existsById(id)) {
            throw new AppException("Invoice not found", HttpStatus.NOT_FOUND);
        }
        invoiceRepository.deleteById(id);
    }

    @Override
    public FileDownloadResponse downloadInvoice(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new AppException("Invoice not found", HttpStatus.NOT_FOUND));

        try {
            Path path = Paths.get(invoice.getFilePath()).normalize();
            if (!Files.exists(path)) {
                throw new AppException("Invoice file not found", HttpStatus.NOT_FOUND);
            }

            Resource resource = new UrlResource(path.toUri());
            return new FileDownloadResponse(resource, invoice.getInvoiceNumber() + ".pdf");
        } catch (MalformedURLException exception) {
            throw new AppException("Error while reading invoice file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private InvoiceData mapToInvoiceData(Order order) {
        List<InvoiceItem> items = new ArrayList<>();
        for (SaleItem item : order.getOrderItemList()) {
            items.add(new InvoiceItem(item.getSaleItemName(), item.getAmount(), item.getSaleItemPrice()));
        }

        return new InvoiceData(order.getOrderId(), order.getTotalPrice(), items);
    }

    private String generatePdf(String invoiceNumber, String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            byte[] pdfBytes = outputStream.toByteArray();
            ProductFile file = fileUploadService.uploadFile(pdfBytes, invoiceNumber + ".pdf", "application/pdf");
            return file.getUrl();
        } catch (Exception exception) {
            throw new AppException("PDF generation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
