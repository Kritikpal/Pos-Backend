package com.kritik.POS.restaurant.util;

import com.kritik.POS.inventory.entity.IngredientStock;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.inventory.entity.MenuItemIngredient;

import java.util.List;

public final class InventoryAvailabilityUtil {

    private InventoryAvailabilityUtil() {
    }

    public static boolean hasRecipe(MenuItem menuItem) {
        return menuItem != null
                && menuItem.getHasRecipe() != null
                && menuItem.getHasRecipe();
    }

    public static Integer computeAvailableServings(MenuItem menuItem) {
        if (menuItem == null) {
            return null;
        }
        if (!hasRecipe(menuItem)) {
            return menuItem.getItemStock() == null ? null : menuItem.getItemStock().getTotalStock();
        }

        int minimumServings = Integer.MAX_VALUE;
        for (MenuItemIngredient ingredientUsage : menuItem.getIngredientUsages()) {
            IngredientStock ingredientStock = ingredientUsage.getIngredientStock();
            if (ingredientStock == null
                    || ingredientUsage.getQuantityRequired() == null
                    || ingredientUsage.getQuantityRequired() <= 0
                    || ingredientStock.getTotalStock() == null
                    || !Boolean.TRUE.equals(ingredientStock.getIsActive())
                    || Boolean.TRUE.equals(ingredientStock.getIsDeleted())) {
                return 0;
            }

            int servingsFromIngredient = (int) Math.floor(ingredientStock.getTotalStock() / ingredientUsage.getQuantityRequired());
            minimumServings = Math.min(minimumServings, servingsFromIngredient);
        }
        return minimumServings == Integer.MAX_VALUE ? 0 : Math.max(minimumServings, 0);
    }

    public static boolean isMenuItemAvailable(MenuItem menuItem) {
        if (menuItem == null
                || !Boolean.TRUE.equals(menuItem.getIsActive())
                || Boolean.TRUE.equals(menuItem.getIsDeleted())) {
            return false;
        }

        if (hasRecipe(menuItem)) {
            Integer availableServings = computeAvailableServings(menuItem);
            return availableServings != null && availableServings > 0;
        }

        if (menuItem.getItemStock() == null) {
            return Boolean.TRUE.equals(menuItem.getIsAvailable());
        }

        return Boolean.TRUE.equals(menuItem.getItemStock().getIsActive())
                && menuItem.getItemStock().getTotalStock() != null
                && menuItem.getItemStock().getTotalStock() > 0
                && Boolean.TRUE.equals(menuItem.getIsAvailable());
    }

    public static boolean isRecipeLowStock(MenuItem menuItem) {
        if (!hasRecipe(menuItem)) {
            return false;
        }
        List<MenuItemIngredient> ingredientUsages = menuItem.getIngredientUsages();
        return ingredientUsages.stream().anyMatch(ingredientUsage -> {
            IngredientStock ingredientStock = ingredientUsage.getIngredientStock();
            return ingredientStock == null
                    || ingredientStock.getTotalStock() == null
                    || ingredientStock.getReorderLevel() == null
                    || ingredientStock.getTotalStock() <= ingredientStock.getReorderLevel();
        });
    }
}
