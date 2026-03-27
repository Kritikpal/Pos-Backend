package com.kritik.POS.restaurant.models.response;

public interface RestaurantProjection {

    String getChainName();
    String getRestaurantId();
    Long getChainId();
    String getResturantName(); // keep same alias spelling
    String getAdminEmail();
    String getCode();
}