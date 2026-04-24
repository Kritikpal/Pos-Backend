package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.entity.stock.IngredientStock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class IngredientResponse {
    private String sku;
    private Long restaurantId;
    private String ingredientName;
    private String description;
    private String category;
    private Double totalStock;
    private Double reorderLevel;
    private String unitOfMeasure;
    private UnitSummaryResponse baseUnit;
    private List<ItemUnitConversionResponse> conversions;
    private Boolean lowStock;
    private Boolean isActive;
    private LocalDateTime lastRestockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private SupplierSummary supplier;

    public static IngredientResponse fromEntity(IngredientStock ingredientStock) {
        IngredientResponse response = new IngredientResponse();
        response.setSku(ingredientStock.getSku());
        response.setRestaurantId(ingredientStock.getRestaurantId());
        response.setIngredientName(ingredientStock.getIngredientName());
        response.setDescription(ingredientStock.getDescription());
        response.setCategory(ingredientStock.getCategory());
        response.setTotalStock(ingredientStock.getTotalStock());
        response.setReorderLevel(ingredientStock.getReorderLevel());
        response.setUnitOfMeasure(ingredientStock.getUnitOfMeasure());
        response.setBaseUnit(UnitSummaryResponse.fromEntity(ingredientStock.getBaseUnit()));
        response.setLowStock(ingredientStock.getTotalStock() != null
                && ingredientStock.getReorderLevel() != null
                && ingredientStock.getTotalStock() <= ingredientStock.getReorderLevel());
        response.setIsActive(ingredientStock.getIsActive());
        response.setLastRestockedAt(ingredientStock.getLastRestockedAt());
        response.setCreatedAt(ingredientStock.getCreatedAt());
        response.setUpdatedAt(ingredientStock.getUpdatedAt());
        if (ingredientStock.getSupplier() != null) {
            response.setSupplier(SupplierSummary.fromEntity(ingredientStock.getSupplier()));
        }
        return response;
    }

    @Data
    public static class SupplierSummary {
        private Long supplierId;
        private String supplierName;
        private String contactPerson;

        public static SupplierSummary fromEntity(com.kritik.POS.inventory.entity.stockEntry.Supplier supplier) {
            SupplierSummary summary = new SupplierSummary();
            summary.setSupplierId(supplier.getSupplierId());
            summary.setSupplierName(supplier.getSupplierName());
            summary.setContactPerson(supplier.getContactPerson());
            return summary;
        }
    }
}
