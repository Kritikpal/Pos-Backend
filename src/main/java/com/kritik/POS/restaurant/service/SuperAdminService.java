package com.kritik.POS.restaurant.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.models.request.RestaurantRequest;
import com.kritik.POS.restaurant.models.request.RestaurantSetupRequest;
import com.kritik.POS.restaurant.models.response.RestaurantChainInfo;
import com.kritik.POS.restaurant.models.response.RestaurantProjection;
import com.kritik.POS.restaurant.models.response.RestaurantSetupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SuperAdminService {

    RestaurantSetupResponse createRestaurantSetup(RestaurantSetupRequest request) throws AppException;

    Long createChain(String chainName) throws AppException;
    Page<RestaurantChainInfo> getAllChains(Long chainId, String search, Pageable pageable) throws AppException;

    RestaurantSetupResponse createRestaurant(RestaurantRequest request, Long chainId) throws AppException;
    Page<RestaurantProjection> getAllRestaurants(Long chainId, Long restaurantId, Boolean isActive, String search, Pageable pageable) throws AppException;

    void createChainAdmin(Long chainId, String email, String phone) throws AppException;
    void createRestaurantAdmin(Long restaurantId, String email, String phone) throws AppException;
}
