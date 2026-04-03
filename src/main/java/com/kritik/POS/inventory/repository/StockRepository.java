package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.ItemStock;
import com.kritik.POS.inventory.projection.StockReceiptSkuProjection;
import com.kritik.POS.inventory.projection.StockSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<ItemStock, String> {

    @EntityGraph(attributePaths = {"menuItem", "menuItem.category", "supplier"})
    Optional<ItemStock> findBySkuAndIsDeletedFalse(String sku);


    @EntityGraph(attributePaths = {"menuItem", "menuItem.category", "supplier"})
    @Query("""
            select s
            from ItemStock s
            where s.isDeleted = false
              and (:skipRestaurantFilter = true or s.restaurantId in :restaurantIds)
            """)
    Page<ItemStock> findVisibleStocks(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                      @Param("restaurantIds") Collection<Long> restaurantIds,
                                      Pageable pageable);

    @Query("""
            select s.sku as sku,
                   s.restaurantId as restaurantId,
                   m.id as menuItemId,
                   m.itemName as itemName,
                   c.categoryId as categoryId,
                   c.categoryName as categoryName,
                   s.totalStock as totalStock,
                   s.reorderLevel as reorderLevel,
                   s.unitOfMeasure as unitOfMeasure,
                   sup.supplierId as supplierId,
                   sup.supplierName as supplierName,
                   s.isActive as isActive,
                   m.isAvailable as isAvailable,
                   s.lastRestockedAt as lastRestockedAt,
                   s.updatedAt as updatedAt
            from ItemStock s
            join s.menuItem m
            join m.category c
            left join s.supplier sup
            where s.isDeleted = false
              and (:skipRestaurantFilter = true or s.restaurantId in :restaurantIds)
              and (:isActive is null or s.isActive = :isActive)
              and (:lowStockOnly = false or s.totalStock <= s.reorderLevel)
              and (
                  coalesce(:search, '') = ''
                  or lower(s.sku) like lower(concat('%', :search, '%'))
                  or lower(m.itemName) like lower(concat('%', :search, '%'))
                  or lower(c.categoryName) like lower(concat('%', :search, '%'))
                  or lower(coalesce(sup.supplierName, '')) like lower(concat('%', :search, '%'))
              )
            order by
                case when s.totalStock <= s.reorderLevel then 0 else 1 end asc,
                s.totalStock asc,
                m.itemName asc
            """)
    Page<StockSummaryProjection> findStockSummaries(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                    @Param("restaurantIds") Collection<Long> restaurantIds,
                                                    @Param("isActive") Boolean isActive,
                                                    @Param("lowStockOnly") boolean lowStockOnly,
                                                    @Param("search") String search,
                                                    Pageable pageable);

    @Query("""
            select s.sku as sku,
                   m.itemName as skuName,
                   'DIRECT_MENU' as skuType,
                    s.totalStock as totalStock,
                    s.unitOfMeasure as unit
            from ItemStock s
            join s.menuItem m
            left join s.supplier sup
            where s.isDeleted = false
              and m.isDeleted = false
              and coalesce(m.hasRecipe, false) = false
              and (:skipRestaurantFilter = true or s.restaurantId in :restaurantIds)
              and (:supplierId is null or sup.supplierId = :supplierId)
            order by lower(m.itemName), s.sku
            """)
    List<StockReceiptSkuProjection> findReceiptSkuOptions(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                          @Param("restaurantIds") Collection<Long> restaurantIds,
                                                          @Param("supplierId") Long supplierId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ItemStock s
            set s.totalStock = s.totalStock - :quantity,
                s.updatedAt = :updatedAt
            where s.sku = :sku
              and s.isDeleted = false
            """)
    int deductStockQuantity(@Param("sku") String sku,
                            @Param("quantity") Integer quantity,
                            @Param("updatedAt") LocalDateTime updatedAt);
}
