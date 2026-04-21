package com.kritik.POS.order.model.response;

import com.kritik.POS.order.entity.SaleItem;
import java.math.BigDecimal;

public record OrderSaleItemResponse(
        Long id,
        Long menuItemId,
        String lineName,
        BigDecimal unitPrice,
        Integer amount,
        BigDecimal lineTotal,
        String taxClassCodeSnapshot,
        boolean priceIncludesTax,
        BigDecimal lineTaxAmount
) {
    public static OrderSaleItemResponse fromEntity(SaleItem saleItem) {
        return new OrderSaleItemResponse(
                saleItem.getSaleItemId(),
                saleItem.getMenuItem() == null ? null : saleItem.getMenuItem().getId(),
                saleItem.getSaleItemName(),
                saleItem.getSaleItemPrice(),
                saleItem.getAmount(),
                saleItem.getLineTotalAmount(),
                saleItem.getTaxClassCodeSnapshot(),
                saleItem.isPriceIncludesTax(),
                saleItem.getLineTaxAmount()
        );
    }
}
