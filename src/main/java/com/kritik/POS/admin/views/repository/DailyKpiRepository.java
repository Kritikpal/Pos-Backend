package com.kritik.POS.admin.views.repository;

import com.kritik.POS.admin.views.entity.DailyKpi;
import com.kritik.POS.admin.views.entity.compositKeys.DailyKpiId;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface DailyKpiRepository
        extends JpaRepository<DailyKpi, DailyKpiId> {

    @Query("""
            SELECT d from DailyKpi d
              where (:skipRestaurantFilter = true or d.id.restaurantId in :restaurantIds)
              and d.id.orderDate = :date
            """)
    List<DailyKpi> findByIdRestaurantId(
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("restaurantIds") Collection<Long> restaurantIds,
            @Param("date") LocalDate date,
            Pageable pageable
    );

    @Query("""
            SELECT 
                COALESCE(SUM(d.successfulOrders), 0),
                COALESCE(SUM(d.canceledOrders), 0),
                COALESCE(SUM(d.totalRevenue), 0),
                COALESCE(AVG(d.avgOrderValue), 0)
            FROM DailyKpi d
            WHERE 
                (:skipFilter = true OR d.id.restaurantId IN :restaurantIds)
                AND d.id.orderDate = :date
            """)
    List<Object[] >getAggregatedDailyKpi(
            boolean skipFilter,
            List<Long> restaurantIds,
            LocalDate date
    );

    List<DailyKpi> findByIdOrderDateBetween(
            LocalDate start, LocalDate end
    );
}