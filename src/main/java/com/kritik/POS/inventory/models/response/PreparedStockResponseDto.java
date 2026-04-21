package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.projection.PreparedStockSummaryProjection;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;

import java.time.LocalDateTime;

public record PreparedStockResponseDto(
        Long menuItemId,
        Long restaurantId,
        String itemName,
        String image,
        String unitCode,
        Double availableQty,
        Double reservedQty,
        Double netAvailableQty,
        Boolean isActive,
        LocalDateTime updatedAt
) {
    public static PreparedStockResponseDto fromProjection(PreparedStockSummaryProjection projection) {
        Double availableQty = projection.getAvailableQty() == null ? 0.0 : projection.getAvailableQty();
        Double reservedQty = projection.getReservedQty() == null ? 0.0 : projection.getReservedQty();
        return new PreparedStockResponseDto(
                projection.getMenuItemId(),
                projection.getRestaurantId(),
                projection.getItemName(),
                projection.getImage() == null ? null : ProductImageUrlUtil.toClientUrl(projection.getImage()),
                projection.getUnitCode() == null || projection.getUnitCode().isBlank() ? "serving" : projection.getUnitCode(),
                availableQty,
                reservedQty,
                Math.max(availableQty - reservedQty, 0.0),
                projection.getIsActive() != null && projection.getIsActive(),
                projection.getUpdatedAt()
        );
    }

    public static PreparedStockResponseDto fromMenuItem(MenuItem menuItem) {
        PreparedItemStock preparedItemStock = menuItem.getPreparedItemStock();
        Double availableQty = preparedItemStock == null || preparedItemStock.getAvailableQty() == null
                ? 0.0
                : preparedItemStock.getAvailableQty();
        Double reservedQty = preparedItemStock == null || preparedItemStock.getReservedQty() == null
                ? 0.0
                : preparedItemStock.getReservedQty();
        String image = menuItem.getProductImage() == null
                ? null
                : ProductImageUrlUtil.toClientUrl(menuItem.getProductImage().getUrl());
        return new PreparedStockResponseDto(
                menuItem.getId(),
                menuItem.getRestaurantId(),
                menuItem.getItemName(),
                image,
                preparedItemStock == null || preparedItemStock.getUnitCode() == null || preparedItemStock.getUnitCode().isBlank()
                        ? "serving"
                        : preparedItemStock.getUnitCode(),
                availableQty,
                reservedQty,
                Math.max(availableQty - reservedQty, 0.0),
                preparedItemStock != null && Boolean.TRUE.equals(preparedItemStock.getActive()),
                preparedItemStock == null ? null : preparedItemStock.getUpdatedAt()
        );
    }
}
