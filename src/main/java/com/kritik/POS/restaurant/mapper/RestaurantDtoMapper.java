package com.kritik.POS.restaurant.mapper;

import com.kritik.POS.restaurant.dto.CategoryResponseDto;
import com.kritik.POS.restaurant.dto.MenuItemResponseDto;
import com.kritik.POS.restaurant.projection.CategorySummaryProjection;
import com.kritik.POS.restaurant.projection.MenuItemSummaryProjection;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;
import com.kritik.POS.tax.util.MoneyUtils;
import org.springframework.stereotype.Component;

@Component
public class RestaurantDtoMapper {


    public MenuItemResponseDto toMenuItemDto(MenuItemSummaryProjection projection) {
        java.math.BigDecimal discountedPrice = projection.getPrice();
        if (projection.getPrice() != null && projection.getDiscount() != null) {
            discountedPrice = MoneyUtils.subtract(
                    projection.getPrice(),
                    MoneyUtils.percentOf(projection.getPrice(), projection.getDiscount())
            );
        }
        return new MenuItemResponseDto(
                projection.getId(),
                projection.getRestaurantId(),
                projection.getSku(),
                ProductImageUrlUtil.toClientUrl(projection.getProductImage()),
                projection.getItemName(),
                projection.getDescription(),
                projection.getPrice(),
                projection.getDiscount(),
                discountedPrice,
                projection.getPriceIncludesTax(),
                projection.getTaxClassId(),
                projection.getIsAvailable(),
                projection.getIsActive(),
                projection.getIsTrending(),
                projection.getMenuType(),
                projection.getRecipeBased(),
                projection.getBatchSize(),
                projection.getTotalStock(),
                projection.getReorderLevel(),
                projection.getUnitOfMeasure(),
                projection.getTotalStock() != null
                        && projection.getReorderLevel() != null
                        && projection.getTotalStock() <= projection.getReorderLevel(),
                projection.getSupplierId(),
                projection.getSupplierName(),
                projection.getCategoryId(),
                projection.getCategoryName(),
                projection.getCreatedAt(),
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
