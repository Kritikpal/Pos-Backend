package com.kritik.POS.restaurant.mapper;

import com.kritik.POS.restaurant.dto.CategoryResponseDto;
import com.kritik.POS.restaurant.dto.MenuItemResponseDto;
import com.kritik.POS.restaurant.projection.CategorySummaryProjection;
import com.kritik.POS.restaurant.projection.MenuItemSummaryProjection;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;
import com.kritik.POS.tax.dto.TaxRateResponseDto;
import com.kritik.POS.tax.projection.TaxRateSummaryProjection;
import org.springframework.stereotype.Component;

@Component
public class RestaurantDtoMapper {


    public MenuItemResponseDto toMenuItemDto(MenuItemSummaryProjection projection) {
        Double discountedPrice = projection.getPrice();
        if (projection.getPrice() != null && projection.getDiscount() != null) {
            discountedPrice = projection.getPrice() - ((projection.getPrice() * projection.getDiscount()) / 100.0);
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
                projection.getIsAvailable(),
                projection.getIsActive(),
                projection.getIsTrending(),
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


    public TaxRateResponseDto toTaxDto(TaxRateSummaryProjection projection) {
        return new TaxRateResponseDto(
                projection.getTaxId(),
                projection.getRestaurantId(),
                projection.getTaxName(),
                projection.getTaxAmount(),
                projection.getIsActive(),
                projection.getCreatedAt(),
                projection.getUpdatedAt()
        );
    }
}
