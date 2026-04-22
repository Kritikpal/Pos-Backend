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

    @Query(value = """
            select item_stock.sku as sku,
                   sum(si.amount) as quantity
            from sale_item si
            join orders o on o.id = si.order_id
            join menu_item menu_item on menu_item.id = si.menu_item_id
            join item_stock item_stock on item_stock.menu_item_id = menu_item.id
            where o.order_id = :orderId
              and o.is_deleted = false
              and si.is_deleted = false
              and menu_item.menu_type = 'DIRECT'
            group by item_stock.sku
            """, nativeQuery = true)
    List<DirectStockDeductionProjection> findDirectStockDeductionsByOrderId(@Param("orderId") String orderId);

    @Query(value = """
            select ingredient_stock.sku as sku,
                   sum((menu_item_ingredient.quantity_required * si.amount) / menu_recipe.batch_size) as quantity
            from sale_item si
            join orders o on o.id = si.order_id
            join menu_item menu_item on menu_item.id = si.menu_item_id
            join menu_item_ingredient menu_item_ingredient on menu_item_ingredient.menu_item_id = menu_item.id
            join ingredient_stock ingredient_stock on ingredient_stock.sku = menu_item_ingredient.ingredient_sku
            join menu_recipe menu_recipe on menu_recipe.id = menu_item_ingredient.recipe_id
            where o.order_id = :orderId
              and o.is_deleted = false
              and si.is_deleted = false
              and menu_item.menu_type = 'RECIPE'
              and menu_recipe.batch_size > 0
            group by ingredient_stock.sku
            """, nativeQuery = true)
    List<IngredientStockDeductionProjection> findIngredientStockDeductionsByOrderId(@Param("orderId") String orderId);

    @Query(value = """
            select distinct menu_item.id
            from sale_item si
            join orders o on o.id = si.order_id
            join menu_item menu_item on menu_item.id = si.menu_item_id
            where o.order_id = :orderId
              and o.is_deleted = false
              and si.is_deleted = false
              and menu_item.menu_type = 'DIRECT'
            """, nativeQuery = true)
    List<Long> findDistinctDirectMenuIdsByOrderId(@Param("orderId") String orderId);

    @Query(value = """
            select distinct menu_item.id
            from sale_item si
            join orders o on o.id = si.order_id
            join menu_item menu_item on menu_item.id = si.menu_item_id
            where o.order_id = :orderId
              and o.is_deleted = false
              and si.is_deleted = false
              and menu_item.menu_type = 'PREPARED'
            """, nativeQuery = true)
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
