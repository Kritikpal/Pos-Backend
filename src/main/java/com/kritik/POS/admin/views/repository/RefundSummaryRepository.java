package com.kritik.POS.admin.views.repository;

import com.kritik.POS.admin.views.entity.DailyKpi;
import com.kritik.POS.admin.views.entity.RefundSummary;
import com.kritik.POS.admin.views.entity.compositKeys.RefundSummaryId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RefundSummaryRepository
        extends JpaRepository<RefundSummary, RefundSummaryId> {

    List<RefundSummary> findByIdRestaurantId(Long restaurantId);

    @Query("""
            SELECT r from RefundSummary r
              where (:skipRestaurantFilter = true or r.id.restaurantId in :restaurantIds)
              and r.id.orderDate = :date
            """)
    List<RefundSummary> findByIdRestaurantId(
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("restaurantIds") Collection<Long> restaurantIds,
            @Param("date") LocalDate date,
            Pageable pageable
    );

    List<RefundSummary> findByIdOrderDateBetween(
            LocalDate start, LocalDate end
    );
}