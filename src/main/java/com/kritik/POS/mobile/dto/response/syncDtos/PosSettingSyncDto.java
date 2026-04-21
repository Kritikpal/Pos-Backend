package com.kritik.POS.mobile.dto.response.syncDtos;

import java.time.LocalDateTime;

public record PosSettingSyncDto(
        Long restaurantId,
        Long chainId,
        String restaurantCode,
        String restaurantName,
        String currency,
        String timezone,
        String gstNumber,
        String phoneNumber,
        String email,
        LocalDateTime updatedAt
) {
}
