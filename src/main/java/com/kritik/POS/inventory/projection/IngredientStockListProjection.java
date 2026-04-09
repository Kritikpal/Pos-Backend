package com.kritik.POS.inventory.projection;

import java.time.LocalDateTime;

public interface IngredientStockListProjection {

    String getSku();

    String getIngredientName();

    Double getTotalStock();

    Double getReorderLevel();

    String getUnitOfMeasure();

    Boolean getIsActive();

    Boolean getIsDeleted();

    Long getRestaurantId();

    LocalDateTime getLastRestockedAt();
}