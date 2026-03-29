package com.kritik.POS.tax.dto;

import java.time.LocalDateTime;

public record TaxRateResponseDto(
        Long taxId,
        Long restaurantId,
        String taxName,
        Double taxAmount,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
