package com.kritik.POS.mobile.dto.response.syncDtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxConfigSyncDto(
        Long taxId,
        Long restaurantId,
        String taxName,
        BigDecimal taxAmount,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
