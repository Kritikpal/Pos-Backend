package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.projection.MenuItemSummaryProjection;
import com.kritik.POS.restaurant.projection.UserDashboardMenuItemProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long>, JpaSpecificationExecutor<MenuItem> {
    List<MenuItem> findAllByIsActiveOrderByIsTrendingDesc(boolean isActive);

    @EntityGraph(attributePaths = {"category", "itemPrice", "itemStock"})
    @Query("""
            SELECT m
            FROM MenuItem m
            join fetch m.ingredientUsages
            WHERE m.isActive = true
              AND m.isDeleted = false
              AND (:skipRestaurantFilter = true OR m.restaurantId IN :restaurantIds)
              AND (:search IS NULL OR :search = '' OR LOWER(m.itemName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR m.category.categoryId = :categoryId)
            ORDER BY m.isTrending DESC, m.createdAt DESC
            """)
    Page<MenuItem> searchDashboardItems(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                        @Param("restaurantIds") Collection<Long> restaurantIds,
                                        @Param("search") String search,
                                        @Param("categoryId") Long categoryId,
                                        Pageable pageable);

    @Query(
            value = """
                    select m.id as id,
                           m.itemName as itemName,
                           c.categoryName as categoryName,
                           m.description as description,
                           ip.price as itemPrice,
                           m.isAvailable as isAvailable,
                           m.isTrending as isTrending,
                           case
                               when m.hasRecipe = true then coalesce(
                                   (
                                       select min(
                                           case
                                               when mi.quantityRequired is null
                                                    or mi.quantityRequired <= 0
                                                    or ing.totalStock is null
                                                    or ing.isActive = false
                                                    or ing.isDeleted = true
                                               then 0
                                               else floor(ing.totalStock / mi.quantityRequired)
                                           end
                                       )
                                       from MenuItemIngredient mi
                                       join mi.ingredientStock ing
                                       where mi.menuItem = m
                                   ),
                                   0
                               )
                               else s.totalStock
                           end as totalStockAvailable
                    from MenuItem m
                    join m.category c
                    left join m.itemPrice ip
                    left join m.itemStock s
                    where m.isActive = true
                      and m.isDeleted = false
                      and (:skipRestaurantFilter = true or m.restaurantId in :restaurantIds)
                      and (:search is null or :search = '' or lower(m.itemName) like lower(concat('%', :search, '%')))
                      and (:categoryId is null or c.categoryId = :categoryId)
                    order by m.isTrending desc, m.createdAt desc
                    """,
            countQuery = """
                    select count(m)
                    from MenuItem m
                    join m.category c
                    where m.isActive = true
                      and m.isDeleted = false
                      and (:skipRestaurantFilter = true or m.restaurantId in :restaurantIds)
                      and (:search is null or :search = '' or lower(m.itemName) like lower(concat('%', :search, '%')))
                      and (:categoryId is null or c.categoryId = :categoryId)
                    """
    )
    Page<UserDashboardMenuItemProjection> findDashboardItems(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                             @Param("restaurantIds") Collection<Long> restaurantIds,
                                                             @Param("search") String search,
                                                             @Param("categoryId") Long categoryId,
                                                             Pageable pageable);

    @Query("""
            select m.id as id,
                   m.restaurantId as restaurantId,
                   s.sku as sku,
                   m.itemName as itemName,
                   m.description as description,
                   ip.price as price,
                   ip.disCount as discount,
                   m.isAvailable as isAvailable,
                   m.isActive as isActive,
                   m.isTrending as isTrending,
                   s.totalStock as totalStock,
                   s.reorderLevel as reorderLevel,
                   s.unitOfMeasure as unitOfMeasure,
                   sup.supplierId as supplierId,
                   sup.supplierName as supplierName,
                   c.categoryId as categoryId,
                   c.categoryName as categoryName,
                   m.createdAt as createdAt,
                   m.updatedAt as updatedAt
            from MenuItem m
            join m.category c
            left join m.itemPrice ip
            left join m.itemStock s
            left join s.supplier sup
            where m.isDeleted = false
              and (:skipRestaurantFilter = true or m.restaurantId in :restaurantIds)
              and (:isActive is null or m.isActive = :isActive)
              and (
                  coalesce(:search, '') = ''
                  or lower(m.itemName) like lower(concat('%', :search, '%'))
                  or lower(c.categoryName) like lower(concat('%', :search, '%'))
                  or lower(coalesce(sup.supplierName, '')) like lower(concat('%', :search, '%'))
              )
            order by m.isTrending desc, m.updatedAt desc, m.createdAt desc
            """)
    Page<MenuItemSummaryProjection> findMenuItemSummaries(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                          @Param("restaurantIds") Collection<Long> restaurantIds,
                                                          @Param("isActive") Boolean isActive,
                                                          @Param("search") String search,
                                                          Pageable pageable);
}
