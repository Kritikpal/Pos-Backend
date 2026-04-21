package com.kritik.POS.order.model.response;

import com.kritik.POS.order.entity.ConfiguredSaleItem;
import com.kritik.POS.order.entity.ConfiguredSaleItemSelection;
import java.math.BigDecimal;

import java.util.List;

public record ConfiguredSaleItemResponse(
        Long id,
        Long configuredTemplateId,
        Long parentMenuItemId,
        String lineName,
        BigDecimal basePrice,
        BigDecimal unitPrice,
        Integer amount,
        BigDecimal lineTotal,
        String taxClassCodeSnapshot,
        boolean priceIncludesTax,
        BigDecimal lineTaxAmount,
        List<ConfiguredSaleItemEntryResponse> items
) {
    public record ConfiguredSaleItemEntryResponse(
            Long id,
            Long slotId,
            String slotName,
            Long childMenuItemId,
            String childItemName,
            Integer quantity,
            BigDecimal priceDelta
    ) {
        public static ConfiguredSaleItemEntryResponse fromEntity(ConfiguredSaleItemSelection selection) {
            return new ConfiguredSaleItemEntryResponse(
                    selection.getId(),
                    selection.getSlotId(),
                    selection.getSlotName(),
                    selection.getChildMenuItemId(),
                    selection.getChildItemName(),
                    selection.getQuantity(),
                    selection.getPriceDelta()
            );
        }
    }

    public static ConfiguredSaleItemResponse fromEntity(ConfiguredSaleItem item) {
        return new ConfiguredSaleItemResponse(
                item.getId(),
                item.getConfiguredTemplateId(),
                item.getParentMenuItemId(),
                item.getLineName(),
                item.getBasePrice(),
                item.getUnitPrice(),
                item.getAmount(),
                item.getLineTotalAmount(),
                item.getTaxClassCodeSnapshot(),
                item.isPriceIncludesTax(),
                item.getLineTaxAmount(),
                item.getSelections().stream()
                        .map(ConfiguredSaleItemEntryResponse::fromEntity)
                        .toList()
        );
    }
}
