package com.kritik.POS.restaurant.dto;

import java.time.LocalDateTime;

public record SupplierResponseDto(
        Long supplierId,
        Long restaurantId,
        String supplierName,
        String contactPerson,
        String phoneNumber,
        String email,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
