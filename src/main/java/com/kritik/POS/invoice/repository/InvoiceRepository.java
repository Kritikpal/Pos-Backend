package com.kritik.POS.invoice.repository;

import com.kritik.POS.invoice.entity.Invoice;
import com.kritik.POS.invoice.model.InvoiceInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    boolean existsByOrderId(Long orderId);

    @Modifying
    @Query(value = """
            delete from invoice i
            using orders o
            where i.order_id = o.id
              and o.restaurant_id = :restaurantId
            """, nativeQuery = true)
    long deleteByOrderRestaurantId(@Param("restaurantId") Long restaurantId);
}
