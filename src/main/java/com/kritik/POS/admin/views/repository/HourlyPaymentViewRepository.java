package com.kritik.POS.admin.views.repository;

import com.kritik.POS.admin.views.entity.HourlyPaymentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface HourlyPaymentViewRepository extends JpaRepository<HourlyPaymentView, String> {

    @Query("""
        SELECT h FROM HourlyPaymentView h
        WHERE h.paymentStatus = :status
          AND (:skipRestaurantFilter = true OR h.restaurantId IN :restaurantIds)
          AND h.paymentDate = :date
        ORDER BY h.hourOfDay
    """)
    List<HourlyPaymentView> findHourlyPayments(
            @Param("status") int status,
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("restaurantIds") Collection<Long> restaurantIds,
            @Param("date") LocalDate date
    );
}