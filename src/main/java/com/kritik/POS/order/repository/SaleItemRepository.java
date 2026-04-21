package com.kritik.POS.order.repository;

import com.kritik.POS.admin.views.projection.MostOrderedMenuProjection;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.model.response.DirectStockDeductionProjection;
import com.kritik.POS.order.model.response.IngredientStockDeductionProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("""
            select itemStock.sku as sku,
                   sum(si.amount) as quantity
            from SaleItem si
            join si.order o
            join si.menuItem menuItem
            join menuItem.itemStock itemStock
            where o.orderId = :orderId
              and o.isDeleted = false
              and si.isDeleted = false
              and menuItem.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.DIRECT
            group by itemStock.sku
            """)
    List<DirectStockDeductionProjection> findDirectStockDeductionsByOrderId(@Param("orderId") String orderId);

    @Query("""
            select ingredientStock.sku as sku,
                   sum((ingredientUsage.quantityRequired * si.amount) / ingredientUsage.recipe.batchSize) as quantity
            from SaleItem si
            join si.order o
            join si.menuItem menuItem
            join menuItem.ingredientUsages ingredientUsage
            join ingredientUsage.ingredientStock ingredientStock
            where o.orderId = :orderId
              and o.isDeleted = false
              and si.isDeleted = false
              and menuItem.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.RECIPE
              and ingredientUsage.recipe.batchSize > 0
            group by ingredientStock.sku
            """)
    List<IngredientStockDeductionProjection> findIngredientStockDeductionsByOrderId(@Param("orderId") String orderId);

    @Query("""
            select distinct menuItem.id
            from SaleItem si
            join si.order o
            join si.menuItem menuItem
            where o.orderId = :orderId
              and o.isDeleted = false
              and si.isDeleted = false
              and menuItem.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.DIRECT
            """)
    List<Long> findDistinctDirectMenuIdsByOrderId(@Param("orderId") String orderId);

    @Query("""
            select distinct menuItem.id
            from SaleItem si
            join si.order o
            join si.menuItem menuItem
            where o.orderId = :orderId
              and o.isDeleted = false
              and si.isDeleted = false
              and menuItem.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED
            """)
    List<Long> findDistinctPreparedMenuIdsByOrderId(@Param("orderId") String orderId);

    @Query("""
            SELECT si.saleItemName as saleItemName,
                SUM(si.amount) as totalQuantity,
                SUM(si.amount * si.saleItemPrice) as totalRevenue
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
    List<MostOrderedMenuProjection> findMostOrderedItemsByPaymentStatusAndDate(@Param("paymentStatus") PaymentStatus paymentStatus,
                                                                               @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                                               @Param("restaurantIds") Collection<Long> restaurantIds,
                                                                               @Param("startDate") LocalDateTime startDate,
                                                                               @Param("endDate") LocalDateTime endDate,
                                                                               Pageable pageable);
}
