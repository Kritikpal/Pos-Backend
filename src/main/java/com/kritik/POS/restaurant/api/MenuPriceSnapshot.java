package com.kritik.POS.restaurant.api;

import java.math.BigDecimal;

public record MenuPriceSnapshot(
        BigDecimal listPrice,
        BigDecimal discountedPrice,
        BigDecimal discountPercent,
        boolean priceIncludesTax
) {
}
