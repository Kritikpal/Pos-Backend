package com.kritik.POS.restaurant.dto;

import com.kritik.POS.restaurant.entity.enums.MenuType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MenuItemResponseDto(
        Long id,
        Long restaurantId,
        String sku,
        String productImage,
        String itemName,
        String description,
        BigDecimal price,
        BigDecimal discount,
        BigDecimal discountedPrice,
        Boolean priceIncludesTax,
        Long taxClassId,
        Boolean isAvailable,
        Boolean isActive,
        Boolean isTrending,
        MenuType menuType,
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
