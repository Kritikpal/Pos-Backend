package com.kritik.POS.inventory.projection;

public interface StockReceiptSkuProjection {
    String getSku();
    String getSkuName();
    Double getTotalStock();
    String getUnit();
    String getSkuType();
}
