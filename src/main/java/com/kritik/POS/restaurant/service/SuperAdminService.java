package com.kritik.POS.restaurant.service;


import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.models.request.RestaurantRequest;
import com.kritik.POS.restaurant.models.request.RestaurantSetupRequest;
import com.kritik.POS.restaurant.models.response.RestaurantChainInfo;
import com.kritik.POS.restaurant.models.response.RestaurantProjection;
import com.kritik.POS.restaurant.models.response.RestaurantSetupResponse;
import org.springframework.data.domain.Page;

public interface SuperAdminService {

    // 🔥 Full setup: Chain + Restaurant + Admin
    RestaurantSetupResponse createRestaurantSetup(RestaurantSetupRequest request) throws AppException;

    // 🏢 Chain Management
    Long createChain(String chainName) throws AppException;
    Page<RestaurantChainInfo> getAllChains() throws AppException;

    // 🍽️ Restaurant Management
    RestaurantSetupResponse createRestaurant(RestaurantRequest request, Long restaurantId) throws AppException;
    Page<RestaurantProjection> getAllRestaurants() throws AppException;

    // 👤 Admin Management
    void createChainAdmin(Long chainId, String email, String phone) throws AppException;
    void createRestaurantAdmin(Long restaurantId, String email, String phone) throws AppException;
}