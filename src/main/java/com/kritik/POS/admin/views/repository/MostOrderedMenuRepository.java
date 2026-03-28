package com.kritik.POS.admin.views.repository;

import com.kritik.POS.admin.views.entity.MostOrderedMenuMV;
import com.kritik.POS.admin.views.entity.compositKeys.MostOrderedMenuId;
import com.kritik.POS.admin.views.projection.MostOrderedMenuProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MostOrderedMenuRepository
        extends JpaRepository<MostOrderedMenuMV, MostOrderedMenuId> {

    @Query("""
    SELECT 
        m.mostOrderedMenuId.saleItemName AS saleItemName,
        SUM(m.totalQuantity) AS totalQuantity,
        SUM(m.totalRevenue) AS totalRevenue
    FROM MostOrderedMenuMV m
    WHERE m.mostOrderedMenuId.paymentStatus = :paymentStatus
      AND (:skipRestaurantFilter = true OR m.mostOrderedMenuId.restaurantId IN :restaurantIds)
      AND m.mostOrderedMenuId.paymentDate BETWEEN :startDate AND :endDate
    GROUP BY m.mostOrderedMenuId.saleItemName
    ORDER BY SUM(m.totalRevenue) DESC
""")
    List<MostOrderedMenuProjection> findTopMenus(
            @Param("paymentStatus") Integer paymentStatus,
            @Param("restaurantIds") List<Long> restaurantIds,
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}