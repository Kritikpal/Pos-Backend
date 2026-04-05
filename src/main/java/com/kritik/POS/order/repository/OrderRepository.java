package com.kritik.POS.order.repository;

import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.response.LastOrderListItemProjection;
import com.kritik.POS.order.model.response.PaymentByHourProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);

    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItemList oi
            left join fetch o.orderTaxes ot
            where o.restaurantId = :restaurantId
              and o.isDeleted = false
            """)
    List<Order> findAllVisibleByRestaurantIdWithDetails(@Param("restaurantId") Long restaurantId);

    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.orderItemList
            WHERE o.orderId = :id
              AND o.isDeleted = false
            """)
    Optional<Order> findByIdWithItems(String id);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItemList oi
            LEFT JOIN FETCH oi.menuItem
            WHERE o.orderId = :orderId
              AND o.isDeleted = false
            """)
    Optional<Order> findByOrderIdWithItems(@Param("orderId") String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItemList oi
            LEFT JOIN FETCH oi.menuItem
            WHERE o.orderId = :orderId
              AND o.isDeleted = false
            """)
    Optional<Order> findByOrderIdWithItemsForUpdate(@Param("orderId") String orderId);

    @Query(value = """
            SELECT
                CAST(EXTRACT(HOUR FROM o.last_updated_time) AS INTEGER) AS hourOfDay,
                COUNT(o.id) AS numberOfPayments
            FROM orders o
            WHERE o.payment_status = :paymentStatus
              AND (:skipRestaurantFilter = true OR o.restaurant_id IN (:restaurantIds))
              AND o.is_deleted = false
              AND o.last_updated_time >= :start
              AND o.last_updated_time < :end
            GROUP BY CAST(EXTRACT(HOUR FROM o.last_updated_time) AS INTEGER)
            ORDER BY CAST(EXTRACT(HOUR FROM o.last_updated_time) AS INTEGER)
            """, nativeQuery = true)
    List<PaymentByHourProjection> countPaymentsByHour(
            @Param("paymentStatus") Integer paymentStatus,
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("restaurantIds") Collection<Long> restaurantIds,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItemList
            WHERE (:orderId IS NULL OR o.orderId = :orderId)
              AND (:skipRestaurantFilter = true OR o.restaurantId IN :restaurantIds)
              AND (:paymentType IS NULL OR o.paymentType = :paymentType)
              AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
              AND o.isDeleted = false
              AND o.lastUpdatedTime BETWEEN :startTime AND :endTime
            ORDER BY o.lastUpdatedTime DESC
            """)
    List<Order> findAllByFilters(@Param("orderId") String orderId,
                                 @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                 @Param("restaurantIds") Collection<Long> restaurantIds,
                                 @Param("paymentType") PaymentType paymentType,
                                 @Param("paymentStatus") PaymentStatus paymentStatus,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime,
                                 Pageable pageable);

//    @Query("""
//            SELECT o FROM Order o
//            WHERE o.paymentStatus = :status
//              AND (:skipRestaurantFilter = true OR o.restaurantId IN :restaurantIds)
//              AND o.isDeleted = false
//            ORDER BY o.lastUpdatedTime DESC
//            """)
//    List<Order> findLastOrders(@Param("status") PaymentStatus status,
//                               @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
//                               @Param("restaurantIds") Collection<Long> restaurantIds,
//                               Pageable pageable);

    @Query(value = """
        SELECT 
            o.order_id AS orderId,
            o.total_price AS totalPrice,
            o.payment_type AS paymentType,
            STRING_AGG(si.sale_item_name || ' * ' || si.amount, ' ') AS description,
            o.last_updated_time AS orderTime
        FROM orders o
        LEFT JOIN sale_item si ON si.order_id = o.id
        WHERE o.payment_status = :status
          AND (:skipRestaurantFilter = true OR o.restaurant_id IN (:restaurantIds))
          AND o.is_deleted = false
        GROUP BY o.id
        ORDER BY o.last_updated_time DESC
        """,
            nativeQuery = true)
    List<LastOrderListItemProjection> findLastOrders(
            @Param("status") Integer status,
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("restaurantIds") Collection<Long> restaurantIds,
            Pageable pageable
    );
    @Query("""
            select count(o)
            from Order o
            where o.paymentStatus = :paymentStatus
              and (:skipRestaurantFilter = true or o.restaurantId in :restaurantIds)
              and o.isDeleted = false
              and o.lastUpdatedTime >= :startTime
              and o.lastUpdatedTime < :endTime
            """)
    long countByPaymentStatusForReportWindow(@Param("paymentStatus") PaymentStatus paymentStatus,
                                             @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                             @Param("restaurantIds") Collection<Long> restaurantIds,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    @Query("""
            select count(o)
            from Order o
            where o.paymentType = :paymentType
              and (:skipRestaurantFilter = true or o.restaurantId in :restaurantIds)
              and o.isDeleted = false
              and o.lastUpdatedTime between :startTime and :endTime
            """)
    long countByPaymentTypeAndLastUpdatedTimeBetween(@Param("paymentType") PaymentType paymentType,
                                                     @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                     @Param("restaurantIds") Collection<Long> restaurantIds,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    @Query("""
            SELECT AVG(o.totalPrice)
            FROM Order o
            WHERE o.lastUpdatedTime >= :startTime
              AND o.lastUpdatedTime < :endTime
              AND o.paymentStatus = :paymentStatus
              AND (:skipRestaurantFilter = true OR o.restaurantId IN :restaurantIds)
              AND o.isDeleted = false
            """)
    Double findAverageOrderValueByPaymentStatusForReportWindow(@Param("startTime") LocalDateTime startTime,
                                                               @Param("endTime") LocalDateTime endTime,
                                                               @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                               @Param("restaurantIds") Collection<Long> restaurantIds,
                                                               @Param("paymentStatus") PaymentStatus paymentStatus);


    @Query("""
            SELECT SUM(o.totalPrice)
            FROM Order o
            WHERE o.lastUpdatedTime >= :startTime
              AND o.lastUpdatedTime < :endTime
              AND o.paymentStatus = :paymentStatus
              AND (:skipRestaurantFilter = true OR o.restaurantId IN :restaurantIds)
              AND o.isDeleted = false
            """)
    Double getTotalAmountByPaymentStatusForReportWindow(@Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime,
                                                        @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                        @Param("restaurantIds") Collection<Long> restaurantIds,
                                                        @Param("paymentStatus") PaymentStatus paymentStatus);

    @Query("""
            SELECT o.totalPrice
            FROM Order o
            WHERE o.paymentStatus = :paymentStatus
              AND (:skipRestaurantFilter = true OR o.restaurantId IN :restaurantIds)
              AND o.isDeleted = false
              AND o.lastUpdatedTime >= :startTime
              AND o.lastUpdatedTime < :endTime
            ORDER BY o.lastUpdatedTime DESC, o.id DESC
            """)
    List<Double> findLatestOrderAmountsByPaymentStatusForReportWindow(@Param("paymentStatus") PaymentStatus paymentStatus,
                                                                      @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                                      @Param("restaurantIds") Collection<Long> restaurantIds,
                                                                      @Param("startTime") LocalDateTime startTime,
                                                                      @Param("endTime") LocalDateTime endTime,
                                                                      Pageable pageable);
}
