package com.kritik.POS.inventory.projection;

public interface PreparedStockSearchProjection {
    Long getMenuId();
    String getItemName();
    String getImage();
    String getUnitCode();
    Double getAvlQuantity();
    Double getRevQuantity();
}
