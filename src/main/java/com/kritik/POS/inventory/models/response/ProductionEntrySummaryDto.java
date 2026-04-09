package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.projection.ProductionEntrySummaryProjection;

import java.time.LocalDateTime;

public record ProductionEntrySummaryDto(
        Long id,
        Long restaurantId,
        Long menuItemId,
        String menuItemName,
        Double producedQty,
        String unitCode,
        LocalDateTime productionTime,
        Long createdBy,
        LocalDateTime createdAt
) {

    public static ProductionEntrySummaryDto fromProjection(ProductionEntrySummaryProjection projection) {
        return new ProductionEntrySummaryDto(
                projection.getId(),
                projection.getRestaurantId(),
                projection.getMenuItemId(),
                projection.getMenuItemName(),
                projection.getProducedQty(),
                projection.getUnitCode(),
                projection.getProductionTime(),
                projection.getCreatedBy(),
                projection.getCreatedAt()
        );
    }
}
