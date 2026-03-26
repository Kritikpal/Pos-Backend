package com.kritik.POS.order.repository;

import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.response.PaymentByHour;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);

    @Query("""
    SELECT o FROM Order o
    LEFT JOIN FETCH o.orderItemList
    WHERE o.orderId = :id
""")
    Optional<Order> findByIdWithItems(String  id);

    @Query("""
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.orderItemList oi
    LEFT JOIN FETCH oi.menuItem
    LEFT JOIN FETCH o.orderTaxes
    WHERE o.orderId = :orderId
""")
    Optional<Order> findByOrderIdWithItems(@Param("orderId") String orderId);

    @Query("SELECT new com.kritik.POS.order.model.response.PaymentByHour(HOUR(o.paymentInitiatedTime), COUNT(o)) " +
            "FROM Order o " +
            "WHERE o.paymentStatus = :status " +
            "AND DATE(o.paymentInitiatedTime) = :date " + // Filter by specific date
            "GROUP BY HOUR(o.paymentInitiatedTime) " +
            "ORDER BY HOUR(o.paymentInitiatedTime)")
    List<PaymentByHour> countPaymentsByHour(
            @Param("status") PaymentStatus paymentStatus,
            @Param("date") LocalDate date // Use LocalDate for date parameter
    );


    @Query("""
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.orderItemList
    WHERE (:orderId IS NULL OR o.orderId = :orderId)
    AND (:paymentType IS NULL OR o.paymentType = :paymentType)
    AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
    AND o.lastUpdatedTime BETWEEN :startTime AND :endTime
    ORDER BY o.lastUpdatedTime DESC
""")
    List<Order> findAllByFilters(
            @Param("orderId") String orderId,
            @Param("paymentType") PaymentType paymentType,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.orderItemList
    WHERE o.paymentStatus = :status
    ORDER BY o.lastUpdatedTime DESC
""")
    List<Order> findLastOrders(@Param("status") PaymentStatus status, Pageable pageable);

    long countByPaymentStatusAndLastUpdatedTimeBetween(PaymentStatus paymentStatus,LocalDateTime startTime,LocalDateTime endTime);
    long countByPaymentTypeAndLastUpdatedTimeBetween(PaymentType paymentType,LocalDateTime startTime,LocalDateTime endTime);

    @Query("SELECT AVG(o.totalPrice) FROM Order o WHERE o.paymentInitiatedTime BETWEEN :startOfDay AND :endOfDay AND o.paymentStatus = :paymentStatus")
    Double findAverageOrderValueByDateAndPaymentType(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );


    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.lastUpdatedTime BETWEEN :startDateTime AND :endDateTime AND o.paymentStatus = :paymentStatus")
    Double getTotalAmountByLastUpdatedTimeAndPaymentStatus(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );




}
