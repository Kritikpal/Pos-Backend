package com.kritik.POS.invoice.repository;

import com.kritik.POS.invoice.entity.Invoice;
import com.kritik.POS.invoice.model.InvoiceInfo;
import com.kritik.POS.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);


    @Query("""
        SELECT i FROM Invoice i
        WHERE (:invoiceNumber IS NULL OR i.invoiceNumber LIKE %:invoiceNumber%)
        ORDER BY i.generatedAt DESC
    """)
    Page<InvoiceInfo> searchInvoices(
            @Param("invoiceNumber") String invoiceNumber,
            Pageable pageable
    );
    boolean existsByOrder(Order order);
}