package com.kritik.POS.order.service.internal;

import java.math.BigDecimal;

public record ConfiguredSelectionInput(
        Long slotId,
        String slotName,
        Long childMenuItemId,
        String childItemName,
        Integer quantity,
        BigDecimal priceDelta
) {
}
