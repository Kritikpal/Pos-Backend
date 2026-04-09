package com.kritik.POS.inventory.projection;

import java.time.LocalDateTime;

public interface ProductionEntrySummaryProjection {
    Long getId();
    Long getRestaurantId();
    Long getMenuItemId();
    String getMenuItemName();
    Double getProducedQty();
    String getUnitCode();
    LocalDateTime getProductionTime();
    Long getCreatedBy();
    LocalDateTime getCreatedAt();
}
