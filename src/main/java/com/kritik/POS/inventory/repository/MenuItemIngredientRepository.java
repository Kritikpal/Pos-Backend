package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.MenuItemIngredient;
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
            select distinct mi.menuItem.id
            from MenuItemIngredient mi
            where mi.ingredientStock.sku in :ingredientSkus
            """)
    List<Long> findDistinctMenuIdsByIngredientStockSkuIn(@Param("ingredientSkus") Collection<String> ingredientSkus);
}
