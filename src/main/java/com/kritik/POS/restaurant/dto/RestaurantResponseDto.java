package com.kritik.POS.restaurant.dto;

public record RestaurantResponseDto(
        Long chainId,
        String chainName,
        Long restaurantId,
        String restaurantName,
        String adminEmail,
        String code
) {
}
