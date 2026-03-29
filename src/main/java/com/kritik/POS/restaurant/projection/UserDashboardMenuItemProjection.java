package com.kritik.POS.restaurant.projection;

public interface UserDashboardMenuItemProjection {
    Long getId();

    String getItemName();

    String getCategoryName();

    String getDescription();

    Double getItemPrice();

    Boolean getIsAvailable();

    Boolean getIsTrending();

    Double getTotalStockAvailable();
}
