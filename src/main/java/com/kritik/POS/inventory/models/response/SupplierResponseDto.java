package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.projection.SupplierSummaryProjection;

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
    public static SupplierResponseDto toSupplierDto(SupplierSummaryProjection projection) {
        return new SupplierResponseDto(
                projection.getSupplierId(),
                projection.getRestaurantId(),
                projection.getSupplierName(),
                projection.getContactPerson(),
                projection.getPhoneNumber(),
                projection.getEmail(),
                projection.getIsActive(),
                projection.getCreatedAt(),
                projection.getUpdatedAt()
        );
    }

}
