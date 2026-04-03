package com.kritik.POS.restaurant.dto;

public record RestaurantChainResponseDto(
        Long chainId,
        String name,
        String description,
        String logoUrl,
        String email,
        String phoneNumber,
        String gstNumber,
        boolean isActive
) {
}
