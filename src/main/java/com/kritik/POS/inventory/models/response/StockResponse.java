package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.entity.stock.ItemStock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class StockResponse {
    private String sku;
    private Long restaurantId;
    private Long menuItemId;
    private String itemName;
    private String description;
    private Integer totalStock;
    private Integer reorderLevel;
    private String unitOfMeasure;
    private UnitSummaryResponse baseUnit;
    private List<ItemUnitConversionResponse> conversions;
    private Boolean lowStock;
    private Boolean isActive;
    private Boolean isAvailable;
    private LocalDateTime lastRestockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private IngredientResponse.SupplierSummary supplier;
    private CategorySummary category;

    public static StockResponse fromEntity(ItemStock itemStock) {
        StockResponse response = new StockResponse();
        response.setSku(itemStock.getSku());
        response.setRestaurantId(itemStock.getRestaurantId());
        response.setMenuItemId(itemStock.getMenuItem().getId());
        response.setItemName(itemStock.getMenuItem().getItemName());
        response.setDescription(itemStock.getMenuItem().getDescription());
        response.setTotalStock(itemStock.getTotalStock());
        response.setReorderLevel(itemStock.getReorderLevel());
        response.setUnitOfMeasure(itemStock.getMenuItem().getBaseUnit() == null
                ? itemStock.getUnitOfMeasure()
                : itemStock.getMenuItem().getBaseUnit().getCode());
        response.setBaseUnit(UnitSummaryResponse.fromEntity(itemStock.getMenuItem().getBaseUnit()));
        response.setLowStock(itemStock.getTotalStock() != null && itemStock.getTotalStock() <= itemStock.getReorderLevel());
        response.setIsActive(itemStock.getIsActive());
        response.setIsAvailable(itemStock.getMenuItem().getIsAvailable());
        response.setLastRestockedAt(itemStock.getLastRestockedAt());
        response.setCreatedAt(itemStock.getCreatedAt());
        response.setUpdatedAt(itemStock.getUpdatedAt());
        response.setCategory(new CategorySummary(
                itemStock.getMenuItem().getCategory().getCategoryId(),
                itemStock.getMenuItem().getCategory().getCategoryName(),
                itemStock.getMenuItem().getCategory().getCategoryDescription(),
                itemStock.getMenuItem().getCategory().getIsActive()
        ));
        if (itemStock.getSupplier() != null) {
            response.setSupplier(IngredientResponse.SupplierSummary.fromEntity(itemStock.getSupplier()));
        }
        return response;
    }

    public record CategorySummary(
            Long categoryId,
            String categoryName,
            String categoryDescription,
            Boolean isActive
    ) {
    }
}
