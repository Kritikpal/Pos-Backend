package com.kritik.POS.restaurant.util;

import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.restaurant.models.request.RestaurantRequest;
import com.kritik.POS.restaurant.models.request.RestaurantSetupRequest;
import com.kritik.POS.restaurant.models.response.RestaurantSetupResponse;
import com.kritik.POS.user.entity.User;

public class RestaurantMapper {

    public static RestaurantChain toChain(RestaurantSetupRequest req) {
        return RestaurantChain.builder()
                .name(req.getChainName())
                .description(req.getDescription())
                .logoUrl(req.getLogoUrl())
                .email(req.getChainEmail())
                .phoneNumber(req.getChainPhone())
                .gstNumber(req.getChainGstNumber())
                .build();
    }

    public static  RestaurantSetupResponse buildResponse(
            RestaurantChain chain,
            Restaurant restaurant,
            String adminEmail
    ) {
        return RestaurantSetupResponse.builder()
                .chainId(chain.getChainId())
                .chainName(chain.getName())
                .restaurantId(restaurant.getRestaurantId())
                .restaurantName(restaurant.getName())
                .adminEmail(adminEmail)
                .build();
    }
    public static Restaurant toRestaurant(RestaurantRequest req, RestaurantChain chain) {
        return Restaurant.builder()
                .name(req.getRestaurantName())
                .code(generateCode(req))
                .addressLine1(req.getAddressLine1())
                .addressLine2(req.getAddressLine2())
                .city(req.getCity())
                .state(req.getState())
                .country(req.getCountry())
                .pincode(req.getPincode())
                .phoneNumber(req.getRestaurantPhone())
                .email(req.getRestaurantEmail())
                .gstNumber(req.getRestaurantGstNumber())
                .chain(chain)
                .build();
    }

    private static String generateCode(RestaurantRequest req) {
        if (req.getCode() != null && !req.getCode().isBlank()) {
            return req.getCode();
        }
        return req.getRestaurantName().substring(0, 3).toUpperCase() + System.currentTimeMillis();
    }
}