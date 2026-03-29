package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.StockReceipt;
import com.kritik.POS.restaurant.projection.StockReceiptSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {

    boolean existsByReceiptNumber(String receiptNumber);

    @EntityGraph(attributePaths = {
            "supplier",
            "receiptItems",
            "receiptItems.itemStock",
            "receiptItems.itemStock.menuItem",
            "receiptItems.itemStock.menuItem.category"
    })
    Optional<StockReceipt> findByReceiptIdAndIsDeletedFalse(Long receiptId);

    @Query("""
            select r.receiptId as receiptId,
                   r.receiptNumber as receiptNumber,
                   r.restaurantId as restaurantId,
                   s.supplierId as supplierId,
                   s.supplierName as supplierName,
                   r.invoiceNumber as invoiceNumber,
                   r.receivedAt as receivedAt,
                   r.totalItems as totalItems,
                   r.totalQuantity as totalQuantity,
                   r.totalCost as totalCost,
                   r.createdAt as createdAt
            from StockReceipt r
            join r.supplier s
            where r.isDeleted = false
              and (:skipRestaurantFilter = true or r.restaurantId in :restaurantIds)
              and (
                  coalesce(:search, '') = ''
                  or lower(r.receiptNumber) like lower(concat('%', :search, '%'))
                  or lower(coalesce(r.invoiceNumber, '')) like lower(concat('%', :search, '%'))
                  or lower(s.supplierName) like lower(concat('%', :search, '%'))
              )
            order by r.receivedAt desc, r.createdAt desc
            """)
    Page<StockReceiptSummaryProjection> findReceiptSummaries(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                             @Param("restaurantIds") Collection<Long> restaurantIds,
                                                             @Param("search") String search,
                                                             Pageable pageable);
}
