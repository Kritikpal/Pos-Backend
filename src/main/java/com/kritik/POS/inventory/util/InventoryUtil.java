package com.kritik.POS.inventory.util;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.IngredientStock;
import com.kritik.POS.inventory.entity.ItemStock;
import com.kritik.POS.inventory.entity.MenuItemIngredient;
import com.kritik.POS.inventory.entity.Supplier;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.MenuItemIngredientRepository;
import com.kritik.POS.inventory.repository.StockRepository;
import com.kritik.POS.inventory.repository.SupplierRepository;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.util.InventoryAvailabilityUtil;
import com.kritik.POS.security.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryUtil {

    private final MenuItemIngredientRepository menuItemIngredientRepository;
    private final SupplierRepository supplierRepository;
    private final TenantAccessService tenantAccessService;
    private final StockRepository stockRepository;
    private final IngredientStockRepository ingredientStockRepository;


    public ItemStock getAccessibleStock(String sku) {
        ItemStock itemStock = stockRepository.findBySkuAndIsDeletedFalse(sku)
                .orElseThrow(() -> new AppException("Stock not found", HttpStatus.BAD_REQUEST));
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(itemStock.getRestaurantId());
        }
        return itemStock;
    }

    public Supplier getAccessibleSupplier(Long supplierId, Long expectedRestaurantId) {
        Supplier supplier = supplierRepository.findBySupplierIdAndIsDeletedFalse(supplierId)
                .orElseThrow(() -> new AppException("Supplier not found", HttpStatus.BAD_REQUEST));
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(supplier.getRestaurantId());
        }
        if (expectedRestaurantId != null && !expectedRestaurantId.equals(supplier.getRestaurantId())) {
            throw new AppException("Supplier does not belong to the selected restaurant", HttpStatus.BAD_REQUEST);
        }
        return supplier;
    }

    public IngredientStock getAccessibleIngredient(String sku) {
        IngredientStock ingredientStock = ingredientStockRepository.findBySkuAndIsDeletedFalse(sku)
                .orElseThrow(() -> new AppException("Ingredient not found", HttpStatus.BAD_REQUEST));
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(ingredientStock.getRestaurantId());
        }
        return ingredientStock;
    }


    public static void syncMenuAvailability(ItemStock itemStock) {
        syncMenuAvailability(itemStock.getMenuItem());
    }

    public static void syncMenuAvailability(MenuItem menuItem) {
        if (menuItem == null) {
            return;
        }
        if (!Boolean.TRUE.equals(menuItem.getIsActive()) || Boolean.TRUE.equals(menuItem.getIsDeleted())) {
            menuItem.setIsAvailable(false);
            return;
        }
        if (InventoryAvailabilityUtil.hasRecipe(menuItem)) {
            menuItem.setIsAvailable(InventoryAvailabilityUtil.isMenuItemAvailable(menuItem));
            return;
        }
        if (menuItem.getItemStock() == null
                || !Boolean.TRUE.equals(menuItem.getItemStock().getIsActive())
                || menuItem.getItemStock().getTotalStock() == null
                || menuItem.getItemStock().getTotalStock() <= 0) {
            menuItem.setIsAvailable(false);
            return;
        }
        menuItem.setIsAvailable(true);
    }

    public void syncMenuAvailabilityForIngredient(String ingredientSku) {
        List<MenuItemIngredient> ingredientUsages = menuItemIngredientRepository.findAllByIngredientStockSku(ingredientSku);
        for (MenuItemIngredient ingredientUsage : ingredientUsages) {
            syncMenuAvailability(ingredientUsage.getMenuItem());
        }
    }

    public static String normalizeSearch(String searchString) {
        return searchString == null ? null : searchString.trim();
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
