package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.restaurant.entity.ItemStock;
import lombok.Data;

@Data
public class StockReport {
    private String sku;
    private Integer stockLeft;
    private Integer reorderLevel;
    private String unitOfMeasure;
    private Boolean lowStock;
    private String menuName;
    private Long menuItemId;
    private Long categoryId;
    private String categoryName;
    private Long supplierId;
    private String supplierName;
    private Integer timesOrder;

    public static StockReport buildStockReport(ItemStock itemStock){
        StockReport stockReport = new StockReport();
        stockReport.setStockLeft(itemStock.getTotalStock());
        stockReport.setSku(itemStock.getSku());
        stockReport.setMenuName(itemStock.getMenuItem().getItemName());
        stockReport.setMenuItemId(itemStock.getMenuItem().getId());
        stockReport.setReorderLevel(itemStock.getReorderLevel());
        stockReport.setUnitOfMeasure(itemStock.getUnitOfMeasure());
        stockReport.setLowStock(itemStock.getTotalStock() != null && itemStock.getTotalStock() <= itemStock.getReorderLevel());
        if (itemStock.getMenuItem().getCategory() != null) {
            stockReport.setCategoryId(itemStock.getMenuItem().getCategory().getCategoryId());
            stockReport.setCategoryName(itemStock.getMenuItem().getCategory().getCategoryName());
        }
        if (itemStock.getSupplier() != null) {
            stockReport.setSupplierId(itemStock.getSupplier().getSupplierId());
            stockReport.setSupplierName(itemStock.getSupplier().getSupplierName());
        }
        return stockReport;
    }

}
