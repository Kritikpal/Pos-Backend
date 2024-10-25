package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.restaurant.DAO.MenuItem;
import com.kritik.POS.tax.TaxRate;
import lombok.Data;
import lombok.Getter;

import java.util.List;


@Getter
public class UserDashboard {

    private final int totalItems;
    private final List<DashBoardItem> menuItemList;
    private final List<ActiveTax> taxRates;

    public UserDashboard(List<MenuItem> menuItemList, List<TaxRate> allByIsActiveTrue) {
        List<DashBoardItem> dashBoardItems = menuItemList.stream().map(DashBoardItem::new).toList();
        this.totalItems = menuItemList.size();
        this.menuItemList = dashBoardItems;
        this.taxRates = allByIsActiveTrue.stream().map(taxRate -> new ActiveTax(taxRate.getTaxName(), taxRate.getTaxAmount())).toList();
    }


    private record ActiveTax(String name, Double taxRate) {
    }

    @Data
    private static class DashBoardItem {
        private final Long id;
        private final String itemName;
        private final String categoryName;
        private final String description;
        private final Double itemPrice;
        private final Boolean isAvailable;
        private final Boolean isTrending;
        private final Integer maxLimit;

        public DashBoardItem(MenuItem menuItem) {
            this.id = menuItem.getId();
            this.itemName = menuItem.getItemName();
            this.description = menuItem.getDescription();
            this.itemPrice = menuItem.getItemPrice().getPrice();
            this.isAvailable = menuItem.getIsAvailable();
            this.isTrending = menuItem.getIsTrending();
            this.categoryName = menuItem.getCategory().getCategoryName();
            this.maxLimit = menuItem.getItemStock().getTotalStock();
        }
    }


}