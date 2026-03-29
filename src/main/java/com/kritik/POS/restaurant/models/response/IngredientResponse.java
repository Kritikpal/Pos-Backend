package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.inventory.entity.IngredientStock;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IngredientResponse {
    private String sku;
    private Long restaurantId;
    private String ingredientName;
    private String description;
    private Double totalStock;
    private Double reorderLevel;
    private String unitOfMeasure;
    private Boolean lowStock;
    private Boolean isActive;
    private LocalDateTime lastRestockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MenuResponse.SupplierSummary supplier;

    public static IngredientResponse fromEntity(IngredientStock ingredientStock) {
        IngredientResponse response = new IngredientResponse();
        response.setSku(ingredientStock.getSku());
        response.setRestaurantId(ingredientStock.getRestaurantId());
        response.setIngredientName(ingredientStock.getIngredientName());
        response.setDescription(ingredientStock.getDescription());
        response.setTotalStock(ingredientStock.getTotalStock());
        response.setReorderLevel(ingredientStock.getReorderLevel());
        response.setUnitOfMeasure(ingredientStock.getUnitOfMeasure());
        response.setLowStock(ingredientStock.getTotalStock() != null
                && ingredientStock.getReorderLevel() != null
                && ingredientStock.getTotalStock() <= ingredientStock.getReorderLevel());
        response.setIsActive(ingredientStock.getIsActive());
        response.setLastRestockedAt(ingredientStock.getLastRestockedAt());
        response.setCreatedAt(ingredientStock.getCreatedAt());
        response.setUpdatedAt(ingredientStock.getUpdatedAt());
        if (ingredientStock.getSupplier() != null) {
            MenuResponse.SupplierSummary supplierSummary = new MenuResponse.SupplierSummary();
            supplierSummary.setSupplierId(ingredientStock.getSupplier().getSupplierId());
            supplierSummary.setSupplierName(ingredientStock.getSupplier().getSupplierName());
            supplierSummary.setContactPerson(ingredientStock.getSupplier().getContactPerson());
            response.setSupplier(supplierSummary);
        }
        return response;
    }
}
