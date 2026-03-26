package com.kritik.POS.restaurant.service.Impl;

import com.kritik.POS.exception.errors.BadRequestException;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.restaurant.models.request.RestaurantRequest;
import com.kritik.POS.restaurant.models.request.RestaurantSetupRequest;
import com.kritik.POS.restaurant.models.response.RestaurantChainInfo;
import com.kritik.POS.restaurant.models.response.RestaurantProjection;
import com.kritik.POS.restaurant.models.response.RestaurantSetupResponse;
import com.kritik.POS.restaurant.repository.RestaurantChainRepository;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.restaurant.service.SuperAdminService;
import com.kritik.POS.restaurant.util.RestaurantMapper;
import com.kritik.POS.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantChainRepository chainRepository;
    private final UserService userService;

    // ==============================
    // 🔥 FULL SETUP
    // ==============================
    @Override
    @Transactional
    public RestaurantSetupResponse createRestaurantSetup(RestaurantSetupRequest req) {

        // ✅ 1. Validate user (delegated)
        userService.validateUserNotExists(req.getAdminEmail());

        // ✅ 2. Create Chain
        RestaurantChain chain = RestaurantMapper.toChain(req);
        chain = chainRepository.save(chain);

        // ✅ 3. Create Restaurant
        if (restaurantRepository.existsByCodeAndChain(req.getCode(), chain)) {
            throw new BadRequestException("Restaurant code already exists in this chain");
        }

        Restaurant restaurant = RestaurantMapper.toRestaurant(req, chain);
        restaurant = restaurantRepository.save(restaurant);

        // ✅ 4. Create Admin (delegated)
        userService.createRestaurantAdmin(
                chain.getChainId(),
                restaurant.getRestaurantId(),
                req.getAdminEmail(),
                req.getAdminPhone(),
                req.getAdminPassword()
        );

        // ✅ 5. Response
        return RestaurantMapper.buildResponse(chain, restaurant, req.getAdminEmail());
    }

    // ==============================
    // 🏢 CREATE CHAIN
    // ==============================
    @Override
    public Long createChain(String chainName) {

        if (chainRepository.existsByName(chainName)) {
            throw new BadRequestException("Chain already exists");
        }

        RestaurantChain chain = new RestaurantChain();
        chain.setName(chainName);

        return chainRepository.save(chain).getChainId();
    }

    @Override
    public Page<RestaurantChainInfo> getAllChains() {
        return chainRepository.findByChainIdOrNameIgnoreCase(
                null, null, null
        );
    }

    // ==============================
    // 🍽️ CREATE RESTAURANT
    // ==============================
    @Override
    @Transactional
    public RestaurantSetupResponse createRestaurant(RestaurantRequest req, Long restaurantId) {

        RestaurantChain chain = chainRepository.findById(restaurantId)
                .orElseThrow(() -> new BadRequestException("Chain not found"));

        if (restaurantRepository.existsByCodeAndChain(req.getCode(), chain)) {
            throw new BadRequestException("Restaurant code already exists");
        }

        Restaurant restaurant = RestaurantMapper.toRestaurant(req, chain);
        restaurant = restaurantRepository.save(restaurant);

        return RestaurantMapper.buildResponse(chain, restaurant, null);
    }

    // ==============================
    // 📋 GET ALL RESTAURANTS
    // ==============================
    @Override
    public Page<RestaurantProjection> getAllRestaurants() {
        return restaurantRepository.findRestaurants(null,
                        null,
                        null,
                        PageRequest.of(0, 10));
    }

    // ==============================
    // 👤 CREATE CHAIN ADMIN
    // ==============================
    @Override
    public void createChainAdmin(Long chainId, String email, String phone) {

        RestaurantChain chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new BadRequestException("Chain not found"));

        userService.createChainAdmin(
                chain.getChainId(),
                email,
                phone,
                "default@123"
        );
    }

    // ==============================
    // 👤 CREATE RESTAURANT ADMIN
    // ==============================
    @Override
    public void createRestaurantAdmin(Long restaurantId, String email, String phone) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BadRequestException("Restaurant not found"));

        userService.createRestaurantAdmin(
                restaurant.getChain().getChainId(),
                restaurant.getRestaurantId(),
                email,
                phone,
                "default@123"
        );
    }
}