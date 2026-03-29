package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.IngredientStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = {"supplier"})
    @Query("""
            select i
            from IngredientStock i
            where i.isDeleted = false
              and (:skipRestaurantFilter = true or i.restaurantId in :restaurantIds)
            order by i.totalStock asc, i.ingredientName asc
            """)
    Page<IngredientStock> findVisibleIngredients(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                 @Param("restaurantIds") Collection<Long> restaurantIds,
                                                 Pageable pageable);

    @EntityGraph(attributePaths = {"supplier"})
    List<IngredientStock> findAllBySkuInAndIsDeletedFalse(Collection<String> skus);
}
