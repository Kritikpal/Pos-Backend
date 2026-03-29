package com.kritik.POS.restaurant.mapper;

import com.kritik.POS.restaurant.dto.CategoryResponseDto;
import com.kritik.POS.restaurant.dto.MenuItemResponseDto;
import com.kritik.POS.restaurant.dto.StockReceiptResponseDto;
import com.kritik.POS.restaurant.dto.StockResponseDto;
import com.kritik.POS.restaurant.dto.SupplierResponseDto;
import com.kritik.POS.restaurant.projection.CategorySummaryProjection;
import com.kritik.POS.restaurant.projection.MenuItemSummaryProjection;
import com.kritik.POS.restaurant.projection.StockReceiptSummaryProjection;
import com.kritik.POS.restaurant.projection.StockSummaryProjection;
import com.kritik.POS.restaurant.projection.SupplierSummaryProjection;
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
                projection.getItemName(),
                projection.getDescription(),
                projection.getPrice(),
                projection.getDiscount(),
                discountedPrice,
                projection.getIsAvailable(),
                projection.getIsActive(),
                projection.getIsTrending(),
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


    public StockResponseDto toStockDto(StockSummaryProjection projection) {
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

    public SupplierResponseDto toSupplierDto(SupplierSummaryProjection projection) {
        return new SupplierResponseDto(
                projection.getSupplierId(),
                projection.getRestaurantId(),
                projection.getSupplierName(),
                projection.getContactPerson(),
                projection.getPhoneNumber(),
                projection.getEmail(),
                projection.getIsActive(),
                projection.getCreatedAt(),
                projection.getUpdatedAt()
        );
    }

    public StockReceiptResponseDto toStockReceiptDto(StockReceiptSummaryProjection projection) {
        return new StockReceiptResponseDto(
                projection.getReceiptId(),
                projection.getReceiptNumber(),
                projection.getRestaurantId(),
                projection.getSupplierId(),
                projection.getSupplierName(),
                projection.getInvoiceNumber(),
                projection.getReceivedAt(),
                projection.getTotalItems(),
                projection.getTotalQuantity(),
                projection.getTotalCost(),
                projection.getCreatedAt()
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
