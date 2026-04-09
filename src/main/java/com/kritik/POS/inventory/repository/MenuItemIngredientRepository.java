package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.projection.MenuItemIngredientProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MenuItemIngredientRepository extends JpaRepository<MenuItemIngredient, Long> {

    @EntityGraph(attributePaths = {"menuItem", "menuItem.itemStock", "ingredientStock"})
    List<MenuItemIngredient> findAllByIngredientStockSku(String ingredientSku);

    @Query("""
        select
            mii.id as id,
            mii.menuItem.id as menuId,
            mii.menuItem.restaurantId as restaurantId,
            mii.menuItem.itemName as menuItemName,
            mii.ingredientStock.sku as ingredientSku,
            mii.ingredientStock.ingredientName as ingredientName,
            mii.quantityRequired as quantityRequired,
            mii.recipe.batchSize as batchSize,
            mii.createdAt as createdAt,
            mii.updatedAt as updatedAt
        from MenuItemIngredient mii
        where (:skipRestaurantFilter = true
               or mii.ingredientStock.restaurantId in :restaurantIds)
    """)
    List<MenuItemIngredientProjection> findAllForRestaurant(
            @Param("skipRestaurantFilter") boolean skipRestaurantFilter,
            @Param("restaurantIds") List<Long> restaurantIds
    );

    @Query("""
            select distinct mi.menuItem.id
            from MenuItemIngredient mi
            where mi.ingredientStock.sku in :ingredientSkus
            """)
    List<Long> findDistinctMenuIdsByIngredientStockSkuIn(@Param("ingredientSkus") Collection<String> ingredientSkus);
}
