package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.projection.StockSummaryProjection;

import java.time.LocalDateTime;

public record StockResponseDto(
        String sku,
        Long restaurantId,
        Long menuItemId,
        String itemName,
        Long categoryId,
        String categoryName,
        Integer totalStock,
        Integer reorderLevel,
        String unitOfMeasure,
        Boolean lowStock,
        Long supplierId,
        String supplierName,
        Boolean isActive,
        Boolean isAvailable,
        LocalDateTime lastRestockedAt,
        LocalDateTime updatedAt
) {
    public static StockResponseDto toStockDto(StockSummaryProjection projection) {
        return new StockResponseDto(
                projection.getSku(),
                projection.getRestaurantId(),
                projection.getMenuItemId(),
                projection.getItemName(),
                projection.getCategoryId(),
                projection.getCategoryName(),
                projection.getTotalStock(),
                projection.getReorderLevel(),
                projection.getUnitOfMeasure(),
                projection.getTotalStock() != null
                        && projection.getReorderLevel() != null
                        && projection.getTotalStock() <= projection.getReorderLevel(),
                projection.getSupplierId(),
                projection.getSupplierName(),
                projection.getIsActive(),
                projection.getIsAvailable(),
                projection.getLastRestockedAt(),
                projection.getUpdatedAt()
        );
    }

}
