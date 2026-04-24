package com.kritik.POS.restaurant.projection;

import com.kritik.POS.restaurant.entity.enums.MenuType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MenuItemSummaryProjection {
    Long getId();
    Long getRestaurantId();
    String getSku();
    String getProductImage();
    String getItemName();
    MenuType getMenuType();
    Long getCategoryId();
    String getCategoryName();
    BigDecimal getPrice();
    Boolean getIsAvailable();
    Boolean getIsActive();
    Integer getTotalStock();
    String getUnitOfMeasure();
    LocalDateTime getUpdatedAt();
}
