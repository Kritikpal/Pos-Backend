package com.kritik.POS.inventory.util;

import com.kritik.POS.inventory.entity.enums.MenuStockStrategy;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.api.StockRequest;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;

import java.util.List;
import java.util.Map;

public class InventoryAvailabilityUtil {


    public static boolean hasRecipe(MenuItem menuItem) {
        MenuType menuType = resolveMenuType(menuItem);
        return menuType == MenuType.RECIPE || menuType == MenuType.PREPARED;
    }

    public static MenuType resolveMenuType(MenuItem menuItem) {
        if (menuItem == null) {
            return MenuType.DIRECT;
        }
        return menuItem.getMenuType() == null ? MenuType.DIRECT : menuItem.getMenuType();
    }

    public static MenuStockStrategy resolveStockStrategy(MenuItem menuItem) {
        return switch (resolveMenuType(menuItem)) {
            case DIRECT -> MenuStockStrategy.DIRECT;
            case RECIPE -> MenuStockStrategy.RECIPE;
            case PREPARED -> MenuStockStrategy.PREPARED;
            case CONFIGURABLE -> MenuStockStrategy.CONFIGURABLE;
        };
    }

    public static Integer computeAvailableServings(MenuItem menuItem) {
        if (menuItem == null) {
            return null;
        }
        return switch (resolveStockStrategy(menuItem)) {
            case DIRECT -> menuItem.getItemStock() == null ? null : menuItem.getItemStock().getTotalStock();
            case PREPARED -> computePreparedAvailableServings(menuItem);
            case RECIPE -> computeRecipeAvailableServings(menuItem);
            case CONFIGURABLE -> null;
        };
    }

    private static Integer computePreparedAvailableServings(MenuItem menuItem) {
        PreparedItemStock preparedItemStock = menuItem.getPreparedItemStock();
        if (preparedItemStock == null || !Boolean.TRUE.equals(preparedItemStock.getActive())) {
            return 0;
        }
        double availableQty = preparedItemStock.getAvailableQty() == null ? 0.0 : preparedItemStock.getAvailableQty();
        double reservedQty = preparedItemStock.getReservedQty() == null ? 0.0 : preparedItemStock.getReservedQty();
        return Math.max((int) Math.floor(availableQty - reservedQty), 0);
    }

    private static Integer computeRecipeAvailableServings(MenuItem menuItem) {
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

        return switch (resolveStockStrategy(menuItem)) {
            case DIRECT -> menuItem.getItemStock() != null
                    && Boolean.TRUE.equals(menuItem.getItemStock().getIsActive())
                    && menuItem.getItemStock().getTotalStock() != null
                    && menuItem.getItemStock().getTotalStock() > 0;
            case PREPARED, RECIPE -> {
                Integer availableServings = computeAvailableServings(menuItem);
                yield availableServings != null && availableServings > 0;
            }
            case CONFIGURABLE -> Boolean.TRUE.equals(menuItem.getIsAvailable());
        };
    }

    public static boolean isLowStock(MenuItem menuItem) {
        if (menuItem == null) {
            return false;
        }
        return switch (resolveStockStrategy(menuItem)) {
            case DIRECT -> menuItem.getItemStock() != null
                    && menuItem.getItemStock().getTotalStock() != null
                    && menuItem.getItemStock().getReorderLevel() != null
                    && menuItem.getItemStock().getTotalStock() <= menuItem.getItemStock().getReorderLevel();
            case PREPARED, RECIPE -> isRecipeLowStock(menuItem);
            case CONFIGURABLE -> false;
        };
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
        return computeRequiredIngredientQuantity(ingredientUsage, servings.doubleValue());
    }

    public static double computeRequiredIngredientQuantity(MenuItemIngredient ingredientUsage, Double servings) {
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

    public static String resolveUnitOfMeasure(MenuItem menuItem) {
        if (menuItem == null) {
            return null;
        }
        return switch (resolveStockStrategy(menuItem)) {
            case DIRECT -> menuItem.getItemStock() == null ? null : menuItem.getItemStock().getUnitOfMeasure();
            case PREPARED -> menuItem.getPreparedItemStock() == null ? "serving" : menuItem.getPreparedItemStock().getUnitCode();
            case RECIPE -> "serving";
            case CONFIGURABLE -> null;
        };
    }

    public static void accumulateStockRequirements(MenuItem menuItem,
                                                   Integer amount,
                                                   List<StockRequest> directStockRequests,
                                                   Map<String, Double> ingredientRequirements,
                                                   Map<Long, Double> preparedRequirements) {
        if (menuItem == null || amount == null || amount <= 0) {
            return;
        }

        switch (resolveStockStrategy(menuItem)) {
            case DIRECT -> {
                if (menuItem.getItemStock() != null) {
                    directStockRequests.add(new StockRequest(menuItem.getItemStock().getSku(), amount));
                }
            }
            case RECIPE -> {
                for (MenuItemIngredient ingredientUsage : menuItem.getIngredientUsages()) {
                    ingredientRequirements.merge(
                            ingredientUsage.getIngredientStock().getSku(),
                            computeRequiredIngredientQuantity(ingredientUsage, amount),
                            Double::sum
                    );
                }
            }
            case PREPARED -> preparedRequirements.merge(menuItem.getId(), amount.doubleValue(), Double::sum);
            case CONFIGURABLE -> {
                // Configurable parent items do not own stock; selected child items drive deduction.
            }
        }
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
