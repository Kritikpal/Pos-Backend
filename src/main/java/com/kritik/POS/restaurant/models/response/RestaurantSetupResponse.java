package com.kritik.POS.restaurant.models.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestaurantSetupResponse {

    private Long chainId;
    private Long restaurantId;

    private String chainName;
    private String restaurantName;

    private String adminEmail;
}