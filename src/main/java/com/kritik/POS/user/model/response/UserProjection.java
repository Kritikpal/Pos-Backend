package com.kritik.POS.user.model.response;

public interface UserProjection {
    Long getUserId();
    String getEmail();
    String getPhoneNumber();
    Long getChainId();
    Long getRestaurantId();
}
