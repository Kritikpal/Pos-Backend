package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.restaurant.projection.UserDashboardMenuItemProjection;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;
import com.kritik.POS.tax.projection.ActiveTaxRateProjection;
import lombok.Getter;

import java.util.List;

@Getter
public class UserDashboard {

    private final int totalItems;
    private final List<DashBoardItem> menuItemList;
    private final List<ActiveTax> taxRates;

    public UserDashboard(List<UserDashboardMenuItemProjection> menuItemList, List<ActiveTaxRateProjection> activeTaxRates) {
        this.menuItemList = menuItemList.stream().map(DashBoardItem::new).toList();
        this.totalItems = menuItemList.size();
        this.taxRates = activeTaxRates.stream().map(ActiveTax::new).toList();
    }

    private record ActiveTax(String name, Double taxRate) {
        private ActiveTax(ActiveTaxRateProjection projection) {
            this(projection.getName(), projection.getTaxRate());
        }
    }

    @Getter
    private static class DashBoardItem {
        private final Long id;
        private final String productImage;
        private final String itemName;
        private final String categoryName;
        private final String description;
        private final Double itemPrice;
        private final Boolean isAvailable;
        private final Boolean isTrending;
        private final MenuType menuType;
        private final Integer totalStockAvailable;

        private DashBoardItem(UserDashboardMenuItemProjection projection) {
            this.id = projection.getId();
            this.productImage = ProductImageUrlUtil.toClientUrl(projection.getProductImage());
            this.itemName = projection.getItemName();
            this.categoryName = projection.getCategoryName();
            this.description = projection.getDescription();
            this.itemPrice = projection.getItemPrice();
            this.isAvailable = projection.getIsAvailable();
            this.isTrending = projection.getIsTrending();
            this.menuType = projection.getMenuType();
            this.totalStockAvailable = projection.getTotalStockAvailable() == null
                    ? null
                    : projection.getTotalStockAvailable().intValue();
        }
    }
}
