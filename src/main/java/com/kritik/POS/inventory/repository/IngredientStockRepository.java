package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.projection.IngredientStockListProjection;
import com.kritik.POS.inventory.projection.StockReceiptSkuProjection;
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

public interface IngredientStockRepository extends JpaRepository<IngredientStock, String> {

    @EntityGraph(attributePaths = {"supplier"})
    Optional<IngredientStock> findBySkuAndIsDeletedFalse(String sku);

    @EntityGraph(attributePaths = {"supplier"})
    @Query("""
            select i
            from IngredientStock i
            left join i.supplier sup
            where i.isDeleted = false
              and (:skipRestaurantFilter = true or i.restaurantId in :restaurantIds)
              and (:isActive is null or i.isActive = :isActive)
              and (:lowStockOnly = false or i.totalStock <= i.reorderLevel)
              and (
                  coalesce(:search, '') = ''
                  or lower(i.sku) like lower(concat('%', :search, '%'))
                  or lower(i.ingredientName) like lower(concat('%', :search, '%'))
                  or lower(coalesce(i.description, '')) like lower(concat('%', :search, '%'))
                  or lower(coalesce(sup.supplierName, '')) like lower(concat('%', :search, '%'))
              )
            order by
                case when i.totalStock <= i.reorderLevel then 0 else 1 end asc,
                i.totalStock asc,
                i.ingredientName asc
            """)
    Page<IngredientStock> findVisibleIngredients(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                 @Param("restaurantIds") Collection<Long> restaurantIds,
                                                 @Param("isActive") Boolean isActive,
                                                 @Param("lowStockOnly") boolean lowStockOnly,
                                                 @Param("search") String search,
                                                 Pageable pageable);

    @Query("""
            select
            i.sku AS sku,
            i.ingredientName AS ingredientName,
            i.totalStock AS totalStock,
            i.reorderLevel AS reorderLevel,
            i.unitOfMeasure AS unitOfMeasure,
            i.isActive AS isActive,
            i.isDeleted AS isDeleted,
            i.restaurantId AS restaurantId,
            i.lastRestockedAt AS lastRestockedAt
            from IngredientStock i
            left join i.supplier sup
            where i.isDeleted = false
              and (:skipRestaurantFilter = true or i.restaurantId in :restaurantIds)
              and (:isActive is null or i.isActive = :isActive)
              and (:lowStockOnly = false or i.totalStock <= i.reorderLevel)
              and (
                  coalesce(:search, '') = ''
                  or lower(i.sku) like lower(concat('%', :search, '%'))
                  or lower(i.ingredientName) like lower(concat('%', :search, '%'))
                  or lower(coalesce(i.description, '')) like lower(concat('%', :search, '%'))
                  or lower(coalesce(sup.supplierName, '')) like lower(concat('%', :search, '%'))
              )
            order by
                case when i.totalStock <= i.reorderLevel then 0 else 1 end asc,
                i.totalStock asc,
                i.ingredientName asc
            """)
    Page<IngredientStockListProjection> findVisibleIngredientsV2(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                                 @Param("restaurantIds") Collection<Long> restaurantIds,
                                                                 @Param("isActive") Boolean isActive,
                                                                 @Param("lowStockOnly") boolean lowStockOnly,
                                                                 @Param("search") String search,
                                                                 Pageable pageable);



    @EntityGraph(attributePaths = {"supplier"})
    List<IngredientStock> findAllBySkuInAndIsDeletedFalse(Collection<String> skus);

    @EntityGraph(attributePaths = {"supplier"})
    List<IngredientStock> findAllByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    @Query("""
            select i.sku as sku,
                   i.ingredientName as skuName,
                   i.totalStock as totalStock,
                   i.unitOfMeasure as unit,
                   'INGREDIENT' as skuType
            from IngredientStock i
            left join i.supplier sup
            where i.isDeleted = false
              and (:skipRestaurantFilter = true or i.restaurantId in :restaurantIds)
              and (:supplierId is null or sup.supplierId = :supplierId)
            order by lower(i.ingredientName), i.sku
            """)
    List<StockReceiptSkuProjection> findReceiptSkuOptions(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                          @Param("restaurantIds") Collection<Long> restaurantIds,
                                                          @Param("supplierId") Long supplierId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update IngredientStock i
            set i.totalStock = i.totalStock - :quantity,
                i.updatedAt = :updatedAt
            where i.sku = :sku
              and i.isDeleted = false
              and i.totalStock >= :quantity
            """)
    int deductStockQuantityIfAvailable(@Param("sku") String sku,
                                       @Param("quantity") Double quantity,
                                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update IngredientStock i
            set i.totalStock = i.totalStock + :quantity,
                i.updatedAt = :updatedAt
            where i.sku = :sku
              and i.isDeleted = false
            """)
    int increaseStockQuantity(@Param("sku") String sku,
                              @Param("quantity") Double quantity,
                              @Param("updatedAt") LocalDateTime updatedAt);
}
