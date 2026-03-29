package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.inventory.entity.ItemStock;
import lombok.Data;

import java.time.LocalDateTime;

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
    private Boolean lowStock;
    private Boolean isActive;
    private Boolean isAvailable;
    private LocalDateTime lastRestockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MenuResponse.SupplierSummary supplier;
    private CategoryResponse category;

    public static StockResponse fromEntity(ItemStock itemStock) {
        StockResponse response = new StockResponse();
        response.setSku(itemStock.getSku());
        response.setRestaurantId(itemStock.getRestaurantId());
        response.setMenuItemId(itemStock.getMenuItem().getId());
        response.setItemName(itemStock.getMenuItem().getItemName());
        response.setDescription(itemStock.getMenuItem().getDescription());
        response.setTotalStock(itemStock.getTotalStock());
        response.setReorderLevel(itemStock.getReorderLevel());
        response.setUnitOfMeasure(itemStock.getUnitOfMeasure());
        response.setLowStock(itemStock.getTotalStock() != null && itemStock.getTotalStock() <= itemStock.getReorderLevel());
        response.setIsActive(itemStock.getIsActive());
        response.setIsAvailable(itemStock.getMenuItem().getIsAvailable());
        response.setLastRestockedAt(itemStock.getLastRestockedAt());
        response.setCreatedAt(itemStock.getCreatedAt());
        response.setUpdatedAt(itemStock.getUpdatedAt());
        response.setCategory(new CategoryResponse(
                itemStock.getMenuItem().getCategory().getCategoryId(),
                itemStock.getMenuItem().getCategory().getCategoryName(),
                itemStock.getMenuItem().getCategory().getCategoryDescription(),
                itemStock.getMenuItem().getCategory().getIsActive()
        ));
        if (itemStock.getSupplier() != null) {
            MenuResponse.SupplierSummary supplierSummary = new MenuResponse.SupplierSummary();
            supplierSummary.setSupplierId(itemStock.getSupplier().getSupplierId());
            supplierSummary.setSupplierName(itemStock.getSupplier().getSupplierName());
            supplierSummary.setContactPerson(itemStock.getSupplier().getContactPerson());
            response.setSupplier(supplierSummary);
        }
        return response;
    }
}
