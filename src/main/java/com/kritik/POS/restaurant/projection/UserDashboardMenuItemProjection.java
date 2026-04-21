package com.kritik.POS.restaurant.projection;

import com.kritik.POS.restaurant.entity.enums.MenuType;

public interface UserDashboardMenuItemProjection {
    Long getId();

    String getProductImage();

    String getItemName();

    String getCategoryName();

    String getDescription();

    Double getItemPrice();

    Boolean getIsAvailable();

    Boolean getIsTrending();

    MenuType getMenuType();

    Double getTotalStockAvailable();
}
