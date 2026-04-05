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
        Integer batchSize = resolveRecipeBatchSize(menuItem);
        if (batchSize == null || batchSize <= 0) {
            return 0;
        }

        double minimumBatchCount = Double.MAX_VALUE;
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

            double batchesFromIngredient = ingredientStock.getTotalStock() / ingredientUsage.getQuantityRequired();
            minimumBatchCount = Math.min(minimumBatchCount, batchesFromIngredient);
        }
        if (minimumBatchCount == Double.MAX_VALUE) {
            return 0;
        }
        return Math.max((int) Math.floor(minimumBatchCount * batchSize), 0);
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

    public static double computeRequiredIngredientQuantity(MenuItemIngredient ingredientUsage, Integer servings) {
        if (ingredientUsage == null || servings == null || servings <= 0) {
            return 0.0;
        }
        Integer batchSize = resolveRecipeBatchSize(ingredientUsage);
        if (batchSize == null || batchSize <= 0
                || ingredientUsage.getQuantityRequired() == null
                || ingredientUsage.getQuantityRequired() <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (ingredientUsage.getQuantityRequired() * servings) / batchSize;
    }

    public static Integer resolveRecipeBatchSize(MenuItemIngredient ingredientUsage) {
        if (ingredientUsage == null) {
            return null;
        }
        if (ingredientUsage.getRecipe() != null) {
            return ingredientUsage.getRecipe().getBatchSize();
        }
        return ingredientUsage.getMenuItem() == null ? null : resolveRecipeBatchSize(ingredientUsage.getMenuItem());
    }

    public static Integer resolveRecipeBatchSize(MenuItem menuItem) {
        if (menuItem == null || menuItem.getRecipe() == null) {
            return null;
        }
        return menuItem.getRecipe().getBatchSize();
    }
}
