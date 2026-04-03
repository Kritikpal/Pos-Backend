package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.entity.enums.StockReceiptSkuType;
import com.kritik.POS.inventory.projection.StockReceiptSkuProjection;

public record StockReceiptSkuOptionDto(
        String sku,
        String skuName,
        StockReceiptSkuType skuType,
        String unit,
        Double availableStock
) {
    public static StockReceiptSkuOptionDto fromProjection(StockReceiptSkuProjection projection) {
        return new StockReceiptSkuOptionDto(
                projection.getSku(),
                projection.getSkuName(),
                StockReceiptSkuType.valueOf(projection.getSkuType()),
                projection.getUnit(),
                projection.getTotalStock()
        );
    }
}
