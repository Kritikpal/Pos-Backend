package com.kritik.POS.order.repository;

import com.kritik.POS.admin.models.response.MostOrderedMenu;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("""
            SELECT new com.kritik.POS.admin.models.response.MostOrderedMenu(
                si.saleItemName,
                SUM(si.amount),
                SUM(si.amount * si.saleItemPrice)
            )
            FROM SaleItem si
            JOIN si.order o
            WHERE o.paymentStatus = :paymentStatus
              AND (:skipRestaurantFilter = true OR si.restaurantId IN :restaurantIds)
              AND si.isDeleted = false
              AND o.isDeleted = false
              AND o.paymentInitiatedTime BETWEEN :startDate AND :endDate
            GROUP BY si.saleItemName
            ORDER BY SUM(si.amount * si.saleItemPrice) DESC
            """)
    List<MostOrderedMenu> findMostOrderedItemsByPaymentStatusAndDate(@Param("paymentStatus") PaymentStatus paymentStatus,
                                                                     @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                                     @Param("restaurantIds") Collection<Long> restaurantIds,
                                                                     @Param("startDate") LocalDateTime startDate,
                                                                     @Param("endDate") LocalDateTime endDate,
                                                                     Pageable pageable);
}
