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
    String getDescription();
    BigDecimal getPrice();
    BigDecimal getDiscount();
    Boolean getPriceIncludesTax();
    Long getTaxClassId();
    Boolean getIsAvailable();
    Boolean getIsActive();
    Boolean getIsTrending();
    MenuType getMenuType();
    Boolean getRecipeBased();
    Integer getBatchSize();
    Integer getTotalStock();
    Integer getReorderLevel();
    String getUnitOfMeasure();
    Long getSupplierId();
    String getSupplierName();
    Long getCategoryId();
    String getCategoryName();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
