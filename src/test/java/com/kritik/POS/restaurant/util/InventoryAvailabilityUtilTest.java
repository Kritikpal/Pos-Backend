package com.kritik.POS.restaurant.util;

import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.util.InventoryAvailabilityUtil;
import com.kritik.POS.restaurant.entity.MenuItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryAvailabilityUtilTest {

    @Test
    void computeAvailableServingsUsesBatchSize() {
        MenuItem menuItem = new MenuItem();
        menuItem.setHasRecipe(true);
        menuItem.setIsActive(true);
        menuItem.setIsDeleted(false);

        MenuRecipe recipe = new MenuRecipe();
        recipe.setMenuItem(menuItem);
        recipe.setBatchSize(10);

        IngredientStock ingredientStock = new IngredientStock();
        ingredientStock.setSku("ING-1");
        ingredientStock.setIngredientName("Rice");
        ingredientStock.setTotalStock(25.0);
        ingredientStock.setIsActive(true);
        ingredientStock.setIsDeleted(false);

        MenuItemIngredient ingredientUsage = new MenuItemIngredient();
        ingredientUsage.setMenuItem(menuItem);
        ingredientUsage.setRecipe(recipe);
        ingredientUsage.setIngredientStock(ingredientStock);
        ingredientUsage.setQuantityRequired(20.0);

        recipe.setIngredientUsages(List.of(ingredientUsage));
        menuItem.setRecipe(recipe);
        menuItem.setIngredientUsages(List.of(ingredientUsage));

        assertThat(InventoryAvailabilityUtil.computeAvailableServings(menuItem)).isEqualTo(12);
        assertThat(InventoryAvailabilityUtil.computeRequiredIngredientQuantity(ingredientUsage, 5)).isEqualTo(10.0);
    }

    @Test
    void computeAvailableServingsUsesPreparedStockForPreparedMenus() {
        MenuItem menuItem = new MenuItem();
        menuItem.setHasRecipe(true);
        menuItem.setIsPrepared(true);
        menuItem.setIsActive(true);
        menuItem.setIsDeleted(false);

        PreparedItemStock preparedItemStock = new PreparedItemStock();
        preparedItemStock.setMenuItemId(10L);
        preparedItemStock.setAvailableQty(7.0);
        preparedItemStock.setReservedQty(2.0);
        preparedItemStock.setUnitCode("PCS");
        preparedItemStock.setActive(true);
        menuItem.setPreparedItemStock(preparedItemStock);

        assertThat(InventoryAvailabilityUtil.computeAvailableServings(menuItem)).isEqualTo(5);
        assertThat(InventoryAvailabilityUtil.isMenuItemAvailable(menuItem)).isTrue();
        assertThat(InventoryAvailabilityUtil.resolveUnitOfMeasure(menuItem)).isEqualTo("PCS");
    }
}
