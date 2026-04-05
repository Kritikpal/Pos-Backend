package com.kritik.POS.inventory.projection;

import java.time.LocalDateTime;

/**
 * Projection for {@link com.kritik.POS.inventory.entity.IngredientStock}
 */
public interface IngredientStockListProjection {
    String getSku();

    String getIngredientName();

    Double getTotalStock();

    Double getReorderLevel();

    String getUnitOfMeasure();

    Boolean isIsActive();

    LocalDateTime getLastRestockedAt();
}