package com.kritik.POS.inventory.projection;

import java.time.LocalDateTime;

public interface StockSummaryProjection {
    String getSku();
    Long getRestaurantId();
    Long getMenuItemId();
    String getItemName();
    Long getCategoryId();
    String getCategoryName();
    Integer getTotalStock();
    Integer getReorderLevel();
    String getUnitOfMeasure();
    Long getSupplierId();
    String getSupplierName();
    Boolean getIsActive();
    Boolean getIsAvailable();
    LocalDateTime getLastRestockedAt();
    LocalDateTime getUpdatedAt();
}
