package com.kritik.POS.restaurant.dto;

public record RestaurantDetailResponseDto(
        Long chainId,
        String chainName,
        Long restaurantId,
        String restaurantName,
        String code,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String pincode,
        String restaurantPhone,
        String restaurantEmail,
        String restaurantGstNumber,
        String currency,
        String timezone,
        boolean isActive
) {
}
