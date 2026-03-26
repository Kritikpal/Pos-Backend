package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.restaurant.entity.RestaurantChain;

/**
 * Projection for {@link RestaurantChain}
 */
public interface RestaurantChainInfo {
    String getName();

    String getLogoUrl();

    String getEmail();

    String getPhoneNumber();
}