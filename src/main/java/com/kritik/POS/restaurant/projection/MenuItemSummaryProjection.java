package com.kritik.POS.restaurant.projection;

import java.time.LocalDateTime;

public interface MenuItemSummaryProjection {
    Long getId();
    Long getRestaurantId();
    String getSku();
    String getItemName();
    String getDescription();
    Double getPrice();
    Double getDiscount();
    Boolean getIsAvailable();
    Boolean getIsActive();
    Boolean getIsTrending();
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
