package com.kritik.POS.inventory.projection;

import java.time.LocalDateTime;

public interface PreparedStockSummaryProjection {
    Long getMenuItemId();
    Long getRestaurantId();
    String getItemName();
    String getImage();
    String getUnitCode();
    Double getAvailableQty();
    Double getReservedQty();
    Boolean getIsActive();
    LocalDateTime getUpdatedAt();
}
