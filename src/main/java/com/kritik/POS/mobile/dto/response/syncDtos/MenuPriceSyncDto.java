package com.kritik.POS.mobile.dto.response.syncDtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MenuPriceSyncDto(
        Long priceId,
        Long menuItemId,
        Long restaurantId,
        BigDecimal price,
        BigDecimal discount,
        Boolean priceIncludesTax,
        LocalDateTime syncUpdatedAt
) {
}
