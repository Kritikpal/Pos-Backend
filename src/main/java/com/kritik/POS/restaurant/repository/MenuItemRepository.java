package com.kritik.POS.restaurant.repository;

import com.kritik.POS.inventory.projection.RecipeMenuItemSearchProjection;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.projection.MenuItemSummaryProjection;
import com.kritik.POS.restaurant.projection.UserDashboardMenuItemProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long>, JpaSpecificationExecutor<MenuItem> {
    List<MenuItem> findAllByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    @EntityGraph(attributePaths = {
            "category",
            "itemPrice",
            "itemStock",
            "itemStock.supplier",
            "preparedItemStock",
            "productImage",
            "recipe",
            "ingredientUsages",
            "ingredientUsages.ingredientStock",
            "ingredientUsages.ingredientStock.supplier"
    })
    @Query("""
            select distinct m
            from MenuItem m
            left join m.ingredientUsages ingredientUsage
            left join ingredientUsage.ingredientStock ingredientStock
            where m.id = :itemId
              and m.isDeleted = false
              and (:skipRestaurantFilter = true or m.restaurantId in :restaurantIds)
            """)
    Optional<MenuItem> findDetailedById(@Param("itemId") Long itemId,
                                        @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                        @Param("restaurantIds") Collection<Long> restaurantIds);

    @EntityGraph(attributePaths = {
            "itemStock",
            "preparedItemStock",
            "recipe",
            "ingredientUsages",
            "ingredientUsages.ingredientStock"
    })
    List<MenuItem> findAllByIdIn(Collection<Long> ids);


    @Query("""
            select m.id as id,
                   m.restaurantId as restaurantId,
                   s.sku as sku,
                   pf.url as productImage,
                   m.itemName as itemName,
                   m.description as description,
                   ip.price as price,
                   ip.disCount as discount,
                   ip.priceIncludesTax as priceIncludesTax,
                   m.taxClassId as taxClassId,
                   m.isAvailable as isAvailable,
                   m.isActive as isActive,
                   m.isTrending as isTrending,
                   m.menuType as menuType,
                   case
                       when m.menuType in (
                           com.kritik.POS.restaurant.entity.enums.MenuType.RECIPE,
                           com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED
                       ) then true
                       else false
                   end as recipeBased,
                   recipe.batchSize as batchSize,
                   case
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.CONFIGURABLE then null
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED then coalesce(
                           (
                               select floor(
                                   case
                                       when pi.active = false then 0
                                       when (coalesce(pi.availableQty, 0) - coalesce(pi.reservedQty, 0)) <= 0 then 0
                                       else (coalesce(pi.availableQty, 0) - coalesce(pi.reservedQty, 0))
                                   end
                               )
                               from PreparedItemStock pi
                               where pi.menuItemId = m.id
                           ),
                           0
                       )
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.RECIPE then coalesce(
                           (
                               select min(
                                   case
                                       when mi.recipe.batchSize is null
                                            or mi.recipe.batchSize <= 0
                                            or mi.quantityRequired is null
                                            or mi.quantityRequired <= 0
                                            or ing.totalStock is null
                                            or ing.isActive = false
                                            or ing.isDeleted = true
                                       then 0
                                       else floor((ing.totalStock / mi.quantityRequired) * mi.recipe.batchSize)
                                   end
                               )
                               from MenuItemIngredient mi
                               join mi.ingredientStock ing
                               where mi.menuItem = m
                           ),
                           0
                       )
                       else s.totalStock
                   end as totalStock,
                   case
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.CONFIGURABLE then null
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED then null
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.RECIPE then null
                       else s.reorderLevel
                   end as reorderLevel,
                   case
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.CONFIGURABLE then null
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED then coalesce(
                           (
                               select pi.unitCode
                               from PreparedItemStock pi
                               where pi.menuItemId = m.id
                           ),
                           'serving'
                       )
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.RECIPE then 'serving'
                       else s.unitOfMeasure
                   end as unitOfMeasure,
                   case
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.CONFIGURABLE then null
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED then null
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.RECIPE then null
                       else sup.supplierId
                   end as supplierId,
                   case
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.CONFIGURABLE then null
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED then null
                       when m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.RECIPE then null
                       else sup.supplierName
                   end as supplierName,
                   c.categoryId as categoryId,
                   c.categoryName as categoryName,
                   m.createdAt as createdAt,
                   m.updatedAt as updatedAt
            from MenuItem m
            join m.category c
            left join m.itemPrice ip
            left join m.itemStock s
            left join m.productImage pf
            left join m.recipe recipe
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

    @Query("""
        select m.id as id,
               m.itemName as itemName,
               pf.url as productImage,
               rc.id as recipeId
        from MenuItem m
        left join m.productImage pf
        left join m.recipe as rc
        where m.isDeleted = false
          and (:skipRestaurantFilter = true or m.restaurantId in :restaurantIds)
          and (
              :types is null
              or m.menuType in :types
          )
          and (
              coalesce(:search, '') = ''
              or lower(m.itemName) like lower(concat('%', :search, '%'))
          )
        order by m.itemName asc
        """)
    List<RecipeMenuItemSearchProjection> searchRecipeMenuItems(
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("restaurantIds") Collection<Long> restaurantIds,
            @Param("types") Collection<MenuType> types,
            @Param("search") String search,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update MenuItem m
            set m.isAvailable = false
            where m.id in :menuIds
            """)
    int markUnavailableByIds(@Param("menuIds") Collection<Long> menuIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update MenuItem m
            set m.isAvailable = true
            where m.id in :menuIds
              and m.isActive = true
              and m.isDeleted = false
              and m.itemStock is not null
              and m.itemStock.isActive = true
              and m.itemStock.totalStock > 0
            """)
    int markDirectMenusAvailableByIds(@Param("menuIds") Collection<Long> menuIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)

    @Query("""
            update MenuItem m
            set m.isAvailable = true
            where m.id in :menuIds
              and m.isActive = true
              and m.isDeleted = false
              and m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED
              and exists (
                  select ps.menuItemId
                  from PreparedItemStock ps
                  where ps.menuItemId = m.id
                    and ps.active = true
                    and (coalesce(ps.availableQty, 0) - coalesce(ps.reservedQty, 0)) > 0
              )
            """)
    int markPreparedMenusAvailableByIds(@Param("menuIds") Collection<Long> menuIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update menu_item m
            set is_available = case
                when m.is_active = false or m.is_deleted = true then false
                when not exists (
                    select 1
                    from menu_item_ingredient mi
                    where mi.menu_item_id = m.id
                ) then false
                when exists (
                    select 1
                    from menu_item_ingredient mi
                    join menu_recipe mr on mr.id = mi.recipe_id
                    join ingredient_stock i on i.sku = mi.ingredient_sku
                    where mi.menu_item_id = m.id
                      and (
                          mr.batch_size is null
                          or mr.batch_size <= 0
                          or i.is_active = false
                          or i.is_deleted = true
                          or i.total_stock is null
                          or mi.quantity_required is null
                          or mi.quantity_required <= 0
                          or floor((i.total_stock / mi.quantity_required) * mr.batch_size) <= 0
                      )
                ) then false
                else true
            end
            where m.id in (:menuIds)
            """, nativeQuery = true)
    int refreshRecipeAvailabilityByIds(@Param("menuIds") Collection<Long> menuIds);

        @Query(
            value = """
                    select m.id as id,
                           pf.url as productImage,
                           m.itemName as itemName,
                           c.categoryName as categoryName,
                           m.description as description,
                           ip.price as itemPrice,
                           m.isAvailable as isAvailable,
                           m.isTrending as isTrending,
                           m.menuType as menuType,
                           CASE
                           WHEN m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.CONFIGURABLE THEN NULL
                           WHEN m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.PREPARED THEN COALESCE(
                            (select
                                case
                                    when pi.active = false then 0
                                    when (coalesce(pi.availableQty, 0) - coalesce(pi.reservedQty, 0)) <= 0 then 0
                                    else (coalesce(pi.availableQty, 0) - coalesce(pi.reservedQty, 0))
                                end
                             from PreparedItemStock pi
                             where pi.menuItemId = m.id),0)
                            WHEN m.menuType = com.kritik.POS.restaurant.entity.enums.MenuType.RECIPE THEN COALESCE(
                            (
                                SELECT MIN(
                                    CASE
                                        WHEN mi.recipe.batchSize IS NULL
                                             OR mi.recipe.batchSize <= 0
                                             OR mi.quantityRequired IS NULL
                                             OR mi.quantityRequired <= 0
                                             OR ing.totalStock IS NULL
                                             OR ing.isActive = false
                                             OR ing.isDeleted = true
                                        THEN 0
                                        ELSE FLOOR((ing.totalStock / mi.quantityRequired) * mi.recipe.batchSize)
                                    END
                                )
                                FROM MenuItemIngredient mi
                                JOIN mi.ingredientStock ing
                                WHERE mi.menuItem = m
                            ),
                            0
                        )
                    
                        ELSE s.totalStock
                    END AS totalStockAvailable
                    from MenuItem m
                    join m.category c
                    left join m.itemPrice ip
                    left join m.itemStock s
                    left join m.productImage pf
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
}
