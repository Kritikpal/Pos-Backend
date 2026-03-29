package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.inventory.entity.StockReceiptItem;
import lombok.Data;

@Data
public class StockReceiptItemResponse {
    private Long receiptItemId;
    private String sku;
    private Long menuItemId;
    private String menuItemName;
    private Long categoryId;
    private String categoryName;
    private Integer quantityReceived;
    private Double unitCost;
    private Double totalCost;

    public static StockReceiptItemResponse fromEntity(StockReceiptItem item) {
        StockReceiptItemResponse response = new StockReceiptItemResponse();
        response.setReceiptItemId(item.getReceiptItemId());
        response.setSku(item.getItemStock().getSku());
        response.setMenuItemId(item.getItemStock().getMenuItem().getId());
        response.setMenuItemName(item.getMenuItemName());
        response.setCategoryId(item.getItemStock().getMenuItem().getCategory().getCategoryId());
        response.setCategoryName(item.getItemStock().getMenuItem().getCategory().getCategoryName());
        response.setQuantityReceived(item.getQuantityReceived());
        response.setUnitCost(item.getUnitCost());
        response.setTotalCost(item.getTotalCost());
        return response;
    }
}
