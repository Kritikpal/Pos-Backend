package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.projection.PreparedStockDeductionProjection;
import com.kritik.POS.inventory.projection.PreparedStockSearchProjection;
import com.kritik.POS.inventory.projection.PreparedStockSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;


public interface PreparedItemStockRepository extends JpaRepository<PreparedItemStock, Long> {

    @Query("""
    select m.id as menuItemId,
           m.restaurantId as restaurantId,
           m.itemName as itemName,
           pf.url as image,
           coalesce(ps.availableQty, 0) as availableQty,
           coalesce(ps.reservedQty, 0) as reservedQty,
           coalesce(ps.unitCode, 'serving') as unitCode,
           coalesce(ps.active, false) as isActive,
           ps.updatedAt as updatedAt
    from MenuItem m
    left join m.preparedItemStock ps
    left join m.productImage pf
    where (:skipRestaurantFilter = true or m.restaurantId in :restaurantIds)
      and (:search is null or :search = '' or lower(m.itemName) like lower(concat('%', :search, '%')))
      and m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED
      and m.isDeleted = false
    order by lower(m.itemName), m.id
""")
    Page<PreparedStockSummaryProjection> findPreparedStockSummaries(
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("restaurantIds") Collection<Long> restaurantIds,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
    select m.id as menuId,
           m.itemName as itemName,
           ps.availableQty as avlQuantity,
           pf.url as image,
           ps.reservedQty as revQuantity,
           ps.unitCode as unitCode
    from MenuItem m
    left join m.preparedItemStock ps
    left join m.productImage pf
    where (:skipRestaurantFilter = true or m.restaurantId in :restaurantIds)
      and (:search is null or :search = '' or lower(m.itemName) like lower(concat('%', :search, '%')))
      and m.isActive = true
      and m.isDeleted = false
""")
    Page<PreparedStockSearchProjection> getPreparedStocks(
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("restaurantIds") Collection<Long> restaurantIds,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select ps.menuItemId as menuItemId,
                   sum(si.amount) as quantity
            from SaleItem si
            join si.order o
            join si.menuItem m
            join PreparedItemStock ps on ps.menuItemId = m.id
            where o.orderId = :orderId
              and o.isDeleted = false
              and si.isDeleted = false
              and m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED
            group by ps.menuItemId
            """)
    List<PreparedStockDeductionProjection> findPreparedStockDeductionsByOrderId(@Param("orderId") String orderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PreparedItemStock ps
            set ps.availableQty = ps.availableQty - :quantity,
                ps.updatedAt = :updatedAt
            where ps.menuItemId = :menuItemId
              and ps.active = true
              and (ps.availableQty - ps.reservedQty) >= :quantity
            """)
    int deductPreparedStockIfAvailable(@Param("menuItemId") Long menuItemId,
                                       @Param("quantity") Double quantity,
                                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PreparedItemStock ps
            set ps.availableQty = ps.availableQty + :quantity,
                ps.updatedAt = :updatedAt
            where ps.menuItemId = :menuItemId
              and ps.active = true
            """)
    int increasePreparedStock(@Param("menuItemId") Long menuItemId,
                              @Param("quantity") Double quantity,
                              @Param("updatedAt") LocalDateTime updatedAt);


}
