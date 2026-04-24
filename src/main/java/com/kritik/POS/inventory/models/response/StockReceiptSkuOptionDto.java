package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.entity.enums.StockReceiptSkuType;

import java.util.List;

public record StockReceiptSkuOptionDto(
        String sku,
        String skuName,
        StockReceiptSkuType skuType,
        String unit,
        Double availableStock,
        UnitSummaryResponse baseUnit,
        List<ItemUnitConversionResponse> purchaseUnits
) {
}
