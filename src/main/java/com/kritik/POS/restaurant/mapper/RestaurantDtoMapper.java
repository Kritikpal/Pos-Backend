package com.kritik.POS.restaurant.mapper;

import com.kritik.POS.restaurant.dto.CategoryResponseDto;
import com.kritik.POS.restaurant.dto.MenuItemResponseDto;
import com.kritik.POS.restaurant.projection.CategorySummaryProjection;
import com.kritik.POS.restaurant.projection.MenuItemSummaryProjection;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;
import org.springframework.stereotype.Component;

@Component
public class RestaurantDtoMapper {


    public MenuItemResponseDto toMenuItemDto(MenuItemSummaryProjection projection) {
        return new MenuItemResponseDto(
                projection.getId(),
                projection.getRestaurantId(),
                projection.getSku(),
                ProductImageUrlUtil.toClientUrl(projection.getProductImage()),
                projection.getItemName(),
                projection.getMenuType(),
                projection.getCategoryId(),
                projection.getCategoryName(),
                projection.getPrice(),
                projection.getIsAvailable(),
                projection.getIsActive(),
                projection.getTotalStock(),
                projection.getUnitOfMeasure(),
                projection.getUpdatedAt()
        );
    }


    public CategoryResponseDto toCategoryDto(CategorySummaryProjection projection) {
        return new CategoryResponseDto(
                projection.getCategoryId(),
                projection.getRestaurantId(),
                projection.getCategoryName(),
                projection.getCategoryDescription(),
                projection.getIsActive(),
                projection.getCreatedAt(),
                projection.getUpdatedAt()
        );
    }
}
