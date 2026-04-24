package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.entity.unit.ItemUnitConversion;

import java.math.BigDecimal;

public record ItemUnitConversionResponse(
        Long id,
        UnitSummaryResponse unit,
        BigDecimal factorToBase,
        Boolean purchaseAllowed,
        Boolean saleAllowed,
        Boolean active
) {
    public static ItemUnitConversionResponse fromEntity(ItemUnitConversion conversion) {
        return new ItemUnitConversionResponse(
                conversion.getId(),
                UnitSummaryResponse.fromEntity(conversion.getUnit()),
                conversion.getFactorToBase(),
                conversion.getPurchaseAllowed(),
                conversion.getSaleAllowed(),
                conversion.getActive()
        );
    }
}
