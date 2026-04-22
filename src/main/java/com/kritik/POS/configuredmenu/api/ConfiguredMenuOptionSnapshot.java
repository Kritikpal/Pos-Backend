package com.kritik.POS.configuredmenu.api;

import java.math.BigDecimal;

public record ConfiguredMenuOptionSnapshot(
        Long id,
        Long childMenuItemId,
        String childItemName,
        BigDecimal priceDelta,
        Integer minQuantity
) {
}
