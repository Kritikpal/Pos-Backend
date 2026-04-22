package com.kritik.POS.order.service.internal;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.api.StockRequest;
import com.kritik.POS.restaurant.api.IngredientUsageSnapshot;
import com.kritik.POS.restaurant.api.MenuItemSnapshot;
import com.kritik.POS.restaurant.api.MenuItemType;
import java.util.Map;
import org.springframework.http.HttpStatus;

public final class OrderMenuSupport {

    private OrderMenuSupport() {
    }

    public static void accumulateStockRequirements(MenuItemSnapshot menuItem,
                                                   Integer amount,
                                                   OrderStockDemand demand) {
        if (menuItem == null || amount == null || amount <= 0) {
            return;
        }

        switch (menuItem.menuType()) {
            case DIRECT -> {
                if (menuItem.directStockSku() != null) {
                    demand.stockRequests().add(new StockRequest(menuItem.directStockSku(), amount));
                    demand.affectedMenuIds().add(menuItem.id());
                }
            }
            case RECIPE -> {
                for (IngredientUsageSnapshot ingredientUsage : menuItem.ingredientUsages()) {
                    Integer batchSize = ingredientUsage.batchSize();
                    Double quantityRequired = ingredientUsage.quantityRequired();
                    if (batchSize == null || batchSize <= 0 || quantityRequired == null || quantityRequired <= 0) {
                        throw new AppException("Recipe stock metadata is incomplete for menu item " + menuItem.itemName(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    double required = (quantityRequired * amount) / batchSize;
                    demand.ingredientRequirements().merge(ingredientUsage.ingredientSku(), required, Double::sum);
                }
                demand.affectedMenuIds().add(menuItem.id());
            }
            case PREPARED -> {
                demand.preparedRequirements().merge(menuItem.id(), amount.doubleValue(), Double::sum);
                demand.affectedMenuIds().add(menuItem.id());
            }
            case CONFIGURABLE -> {
                // Child selections drive stock deductions for configurable items.
            }
        }
    }

    public static MenuItemType resolveMenuType(MenuItemSnapshot menuItem) {
        return menuItem == null || menuItem.menuType() == null ? MenuItemType.DIRECT : menuItem.menuType();
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static void mergeDemands(OrderStockDemand target, OrderStockDemand source) {
        target.stockRequests().addAll(source.stockRequests());
        for (Map.Entry<String, Double> entry : source.ingredientRequirements().entrySet()) {
            target.ingredientRequirements().merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        for (Map.Entry<Long, Double> entry : source.preparedRequirements().entrySet()) {
            target.preparedRequirements().merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        target.affectedMenuIds().addAll(source.affectedMenuIds());
    }
}
