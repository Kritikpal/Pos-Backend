package com.kritik.POS.mobile.dto.response.syncDtos;

import java.time.LocalDateTime;

public record TaxClassSyncDto(
        Long id,
        Long restaurantId,
        String code,
        String name,
        String description,
        Boolean isExempt,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
