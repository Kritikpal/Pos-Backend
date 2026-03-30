package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.inventory.entity.IngredientStock;
import com.kritik.POS.inventory.entity.ItemStock;
import com.kritik.POS.inventory.entity.enums.StockReceiptSkuType;
import com.kritik.POS.inventory.entity.StockReceiptItem;
import lombok.Data;

@Data
public class StockReceiptItemResponse {
    private Long receiptItemId;
    private String sku;
    private StockReceiptSkuType skuType;
    private String skuName;
    private Long menuItemId;
    private Long categoryId;
    private String categoryName;
    private Integer quantityReceived;
    private Double unitCost;
    private Double totalCost;

    public static StockReceiptItemResponse fromEntity(StockReceiptItem item) {
        StockReceiptItemResponse response = new StockReceiptItemResponse();
        response.setReceiptItemId(item.getReceiptItemId());
        response.setSkuType(item.getSkuType());
        response.setSkuName(item.getSkuName());

        ItemStock itemStock = item.getItemStock();
        IngredientStock ingredientStock = item.getIngredientStock();
        if (itemStock != null) {
            response.setSku(itemStock.getSku());
            response.setSkuType(response.getSkuType() == null ? StockReceiptSkuType.DIRECT_MENU : response.getSkuType());
            response.setSkuName(response.getSkuName() == null ? itemStock.getMenuItem().getItemName() : response.getSkuName());
            response.setMenuItemId(itemStock.getMenuItem().getId());
            response.setCategoryId(itemStock.getMenuItem().getCategory().getCategoryId());
            response.setCategoryName(itemStock.getMenuItem().getCategory().getCategoryName());
        } else if (ingredientStock != null) {
            response.setSku(ingredientStock.getSku());
            response.setSkuType(response.getSkuType() == null ? StockReceiptSkuType.INGREDIENT : response.getSkuType());
            response.setSkuName(response.getSkuName() == null ? ingredientStock.getIngredientName() : response.getSkuName());
        }

        response.setQuantityReceived(item.getQuantityReceived());
        response.setUnitCost(item.getUnitCost());
        response.setTotalCost(item.getTotalCost());
        return response;
    }
}
