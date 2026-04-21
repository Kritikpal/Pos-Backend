package com.kritik.POS.tax.dto;

import java.time.LocalDateTime;

public record TaxClassResponseDto(
        Long id,
        Long restaurantId,
        String code,
        String name,
        String description,
        boolean isExempt,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
