package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.restaurant.entity.ItemStock;
import lombok.Data;

@Data
public class StockReport {
    private String sku;
    private Integer stockLeft;
    private String menuName;
    private Integer timesOrder;

    public static StockReport buildStockReport(ItemStock itemStock){
        StockReport stockReport = new StockReport();
        stockReport.setStockLeft(itemStock.getTotalStock());
        stockReport.setSku(itemStock.getSku());
        stockReport.setMenuName(itemStock.getMenuItem().getItemName());
        return stockReport;
    }

}
