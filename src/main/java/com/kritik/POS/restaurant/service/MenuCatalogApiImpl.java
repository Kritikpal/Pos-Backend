package com.kritik.POS.restaurant.service;

import com.kritik.POS.common.util.MoneyUtils;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.restaurant.api.IngredientUsageSnapshot;
import com.kritik.POS.restaurant.api.MenuCatalogApi;
import com.kritik.POS.restaurant.api.MenuItemSnapshot;
import com.kritik.POS.restaurant.api.MenuItemType;
import com.kritik.POS.restaurant.api.MenuPriceSnapshot;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuCatalogApiImpl implements MenuCatalogApi {

    private final MenuItemRepository menuItemRepository;
    private final TenantAccessService tenantAccessService;

    @Override
    public MenuItemSnapshot getAccessibleMenuItem(Long menuItemId) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        MenuItem menuItem = menuItemRepository.findDetailedById(
                        menuItemId,
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds))
                .orElseThrow(() -> new AppException("Menu Item is not valid", HttpStatus.BAD_REQUEST));

        return toSnapshot(menuItem);
    }

    private MenuItemSnapshot toSnapshot(MenuItem menuItem) {
        return new MenuItemSnapshot(
                menuItem.getId(),
                menuItem.getRestaurantId(),
                menuItem.getTaxClassId(),
                menuItem.getItemName(),
                menuItem.getDescription(),
                Boolean.TRUE.equals(menuItem.getIsActive()),
                Boolean.TRUE.equals(menuItem.getIsDeleted()),
                Boolean.TRUE.equals(menuItem.getIsAvailable()),
                toMenuItemType(menuItem.getMenuType()),
                toPriceSnapshot(menuItem.getItemPrice()),
                menuItem.getItemStock() == null ? null : menuItem.getItemStock().getSku(),
                menuItem.getIngredientUsages().stream().map(this::toIngredientUsageSnapshot).toList()
        );
    }

    private IngredientUsageSnapshot toIngredientUsageSnapshot(MenuItemIngredient ingredientUsage) {
        return new IngredientUsageSnapshot(
                ingredientUsage.getIngredientStock().getSku(),
                ingredientUsage.getQuantityRequired(),
                ingredientUsage.getRecipe() == null ? null : ingredientUsage.getRecipe().getBatchSize()
        );
    }

    private MenuPriceSnapshot toPriceSnapshot(ItemPrice itemPrice) {
        if (itemPrice == null) {
            return new MenuPriceSnapshot(MoneyUtils.zero(), MoneyUtils.zero(), null, false);
        }
        BigDecimal listPrice = MoneyUtils.money(itemPrice.getPrice());
        BigDecimal discountedPrice = listPrice;
        if (itemPrice.getDisCount() != null && itemPrice.getDisCount().compareTo(BigDecimal.ZERO) != 0) {
            discountedPrice = MoneyUtils.subtract(listPrice, MoneyUtils.percentOf(listPrice, itemPrice.getDisCount()));
        }
        return new MenuPriceSnapshot(
                listPrice,
                discountedPrice,
                itemPrice.getDisCount(),
                Boolean.TRUE.equals(itemPrice.getPriceIncludesTax())
        );
    }

    private MenuItemType toMenuItemType(MenuType menuType) {
        if (menuType == null) {
            return MenuItemType.DIRECT;
        }
        return MenuItemType.valueOf(menuType.name());
    }
}
