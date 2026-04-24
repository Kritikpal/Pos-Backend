package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.inventory.entity.enums.MenuStockStrategy;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.models.response.UnitSummaryResponse;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.inventory.util.InventoryAvailabilityUtil;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;
import com.kritik.POS.restaurant.util.RestaurantUtil;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MenuResponse {
    private Long id;
    private String sku;
    private String productImage;
    private String itemName;
    private String description;
    private Long taxClassId;
    private MenuItemPrice itemPrice;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
    private Boolean isActive;
    private Boolean isTrending;
    private Boolean isPrepared;
    private MenuType menuType;
    private CategoryResponse category;
    private Integer itemInStock;
    private Integer reorderLevel;
    private String unitOfMeasure;
    private UnitSummaryResponse baseUnit;
    private Double availableQty;
    private Double reservedQty;
    private String unit;
    private Boolean lowStock;
    private SupplierSummary supplier;
    private Boolean recipeBased;
    private Integer batchSize;
    private List<IngredientUsageSummary> ingredients = new ArrayList<>();

    @Data
    public static class SupplierSummary {
        private Long supplierId;
        private String supplierName;
        private String contactPerson;
    }

    @Data
    public static class IngredientUsageSummary {
        private String ingredientSku;
        private String ingredientName;
        private Double quantityRequired;
        private Double availableStock;
        private String unitOfMeasure;
        private Boolean lowStock;
        private Long supplierId;
        private String supplierName;
    }

    @Data
    public static class MenuItemPrice {
        private BigDecimal price;
        private BigDecimal disCountedPrice;
        private BigDecimal disCount;
        private Boolean priceIncludesTax;

        public MenuItemPrice(ItemPrice itemPrice) {
            this.price = itemPrice.getPrice();
            this.disCount = itemPrice.getDisCount();
            this.disCountedPrice = RestaurantUtil.getMenuItemPrice(itemPrice);
            this.priceIncludesTax = itemPrice.getPriceIncludesTax();
        }
    }

    public static MenuResponse buildResponseFromMenu(MenuItem menuItem) {
        MenuResponse menuResponse = new MenuResponse();
        menuResponse.setId(menuItem.getId());
        menuResponse.setSku(menuItem.getItemStock() == null ? null : menuItem.getItemStock().getSku());
        menuResponse.setProductImage(menuItem.getProductImage() == null
                ? null
                : ProductImageUrlUtil.toClientUrl(menuItem.getProductImage().getUrl()));
        menuResponse.setItemName(menuItem.getItemName());
        menuResponse.setDescription(menuItem.getDescription());
        menuResponse.setTaxClassId(menuItem.getTaxClassId());
        menuResponse.setItemPrice(new MenuItemPrice(menuItem.getItemPrice()));
        menuResponse.setIsAvailable(menuItem.getIsAvailable());
        menuResponse.setCreatedAt(menuItem.getCreatedAt());
        menuResponse.setIsActive(menuItem.getIsActive());
        menuResponse.setIsTrending(menuItem.getIsTrending());
        menuResponse.setIsPrepared(InventoryAvailabilityUtil.resolveMenuType(menuItem) == MenuType.PREPARED);
        menuResponse.setMenuType(InventoryAvailabilityUtil.resolveMenuType(menuItem));
        menuResponse.setCategory(new CategoryResponse(menuItem.getCategory().getCategoryId(),
                menuItem.getCategory().getCategoryName(),
                menuItem.getCategory().getCategoryDescription(),
                menuItem.getCategory().getIsActive()));
        menuResponse.setRecipeBased(InventoryAvailabilityUtil.hasRecipe(menuItem));
        menuResponse.setBaseUnit(UnitSummaryResponse.fromEntity(menuItem.getBaseUnit()));
        PreparedItemStock preparedItemStock = menuItem.getPreparedItemStock();
        MenuStockStrategy stockStrategy = InventoryAvailabilityUtil.resolveStockStrategy(menuItem);
        if (stockStrategy == MenuStockStrategy.RECIPE || stockStrategy == MenuStockStrategy.PREPARED) {
            menuResponse.setBatchSize(menuItem.getRecipe() == null ? null : menuItem.getRecipe().getBatchSize());
            menuResponse.setItemInStock(InventoryAvailabilityUtil.computeAvailableServings(menuItem));
            menuResponse.setUnitOfMeasure(InventoryAvailabilityUtil.resolveUnitOfMeasure(menuItem));
            menuResponse.setAvailableQty(resolveAvailableQty(menuItem, preparedItemStock));
            menuResponse.setReservedQty(resolveReservedQty(preparedItemStock));
            menuResponse.setUnit(resolveResponseUnit(menuItem, preparedItemStock));
            menuResponse.setLowStock(InventoryAvailabilityUtil.isLowStock(menuItem));
            menuResponse.setIngredients(menuItem.getIngredientUsages().stream()
                    .map(MenuResponse::buildIngredientUsageSummary)
                    .toList());
        } else if (stockStrategy == MenuStockStrategy.DIRECT) {
            ItemStock itemStock = menuItem.getItemStock();
            if (itemStock != null) {
                menuResponse.setItemInStock(itemStock.getTotalStock());
                menuResponse.setReorderLevel(itemStock.getReorderLevel());
                menuResponse.setUnitOfMeasure(itemStock.getUnitOfMeasure());
                menuResponse.setAvailableQty(itemStock.getTotalStock() == null ? null : itemStock.getTotalStock().doubleValue());
                menuResponse.setReservedQty(0.0);
                menuResponse.setUnit(itemStock.getUnitOfMeasure());
                menuResponse.setLowStock(InventoryAvailabilityUtil.isLowStock(menuItem));
                if (itemStock.getSupplier() != null) {
                    SupplierSummary supplierSummary = new SupplierSummary();
                    supplierSummary.setSupplierId(itemStock.getSupplier().getSupplierId());
                    supplierSummary.setSupplierName(itemStock.getSupplier().getSupplierName());
                    supplierSummary.setContactPerson(itemStock.getSupplier().getContactPerson());
                    menuResponse.setSupplier(supplierSummary);
                }
            }
        } else {
            menuResponse.setItemInStock(null);
            menuResponse.setReorderLevel(null);
            menuResponse.setUnitOfMeasure(null);
            menuResponse.setAvailableQty(null);
            menuResponse.setReservedQty(0.0);
            menuResponse.setUnit(null);
            menuResponse.setLowStock(false);
        }
        return menuResponse;
    }

    private static Double resolveAvailableQty(MenuItem menuItem, PreparedItemStock preparedItemStock) {
        if (preparedItemStock != null) {
            return preparedItemStock.getAvailableQty() == null ? 0.0 : preparedItemStock.getAvailableQty();
        }
        Integer itemInStock = InventoryAvailabilityUtil.computeAvailableServings(menuItem);
        return itemInStock == null ? null : itemInStock.doubleValue();
    }

    private static Double resolveReservedQty(PreparedItemStock preparedItemStock) {
        if (preparedItemStock == null) {
            return 0.0;
        }
        return preparedItemStock.getReservedQty() == null ? 0.0 : preparedItemStock.getReservedQty();
    }

    private static String resolveResponseUnit(MenuItem menuItem, PreparedItemStock preparedItemStock) {
        if (preparedItemStock != null && preparedItemStock.getUnitCode() != null) {
            return preparedItemStock.getUnitCode();
        }
        return InventoryAvailabilityUtil.resolveUnitOfMeasure(menuItem);
    }

    private static IngredientUsageSummary buildIngredientUsageSummary(MenuItemIngredient ingredientUsage) {
        IngredientUsageSummary summary = new IngredientUsageSummary();
        summary.setIngredientSku(ingredientUsage.getIngredientStock().getSku());
        summary.setIngredientName(ingredientUsage.getIngredientStock().getIngredientName());
        summary.setQuantityRequired(ingredientUsage.getQuantityRequired());
        summary.setAvailableStock(ingredientUsage.getIngredientStock().getTotalStock());
        summary.setUnitOfMeasure(ingredientUsage.getIngredientStock().getUnitOfMeasure());
        summary.setLowStock(ingredientUsage.getIngredientStock().getTotalStock() != null
                && ingredientUsage.getIngredientStock().getReorderLevel() != null
                && ingredientUsage.getIngredientStock().getTotalStock() <= ingredientUsage.getIngredientStock().getReorderLevel());
        if (ingredientUsage.getIngredientStock().getSupplier() != null) {
            summary.setSupplierId(ingredientUsage.getIngredientStock().getSupplier().getSupplierId());
            summary.setSupplierName(ingredientUsage.getIngredientStock().getSupplier().getSupplierName());
        }
        return summary;
    }


}
