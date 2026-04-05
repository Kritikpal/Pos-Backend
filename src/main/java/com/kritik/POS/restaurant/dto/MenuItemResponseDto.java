package com.kritik.POS.restaurant.dto;

import java.time.LocalDateTime;

public record MenuItemResponseDto(
        Long id,
        Long restaurantId,
        String sku,
        String productImage,
        String itemName,
        String description,
        Double price,
        Double discount,
        Double discountedPrice,
        Boolean isAvailable,
        Boolean isActive,
        Boolean isTrending,
        Boolean recipeBased,
        Integer batchSize,
        Integer totalStock,
        Integer reorderLevel,
        String unitOfMeasure,
        Boolean lowStock,
        Long supplierId,
        String supplierName,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
