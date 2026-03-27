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
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantChainRepository chainRepository;
    private final UserService userService;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public RestaurantSetupResponse createRestaurantSetup(RestaurantSetupRequest req) {
        tenantAccessService.assertSuperAdmin();
        userService.validateUserNotExists(req.getAdminEmail());

        RestaurantChain chain = RestaurantMapper.toChain(req);
        chain = chainRepository.save(chain);

        if (restaurantRepository.existsByCodeAndChain(req.getCode(), chain)) {
            throw new BadRequestException("Restaurant code already exists in this chain");
        }

        Restaurant restaurant = RestaurantMapper.toRestaurant(req, chain);
        restaurant = restaurantRepository.save(restaurant);

        userService.createRestaurantAdmin(
                chain.getChainId(),
                restaurant.getRestaurantId(),
                req.getAdminEmail(),
                req.getAdminPhone(),
                req.getAdminPassword()
        );

        return RestaurantMapper.buildResponse(chain, restaurant, req.getAdminEmail());
    }

    @Override
    public Long createChain(String chainName) {
        tenantAccessService.assertSuperAdmin();
        if (chainRepository.existsByName(chainName)) {
            throw new BadRequestException("Chain already exists");
        }

        RestaurantChain chain = new RestaurantChain();
        chain.setName(chainName);

        return chainRepository.save(chain).getChainId();
    }

    @Override
    public Page<RestaurantChainInfo> getAllChains(Long chainId, String search, Pageable pageable) {
        Long accessibleChainId = tenantAccessService.resolveAccessibleChainId(chainId);
        return chainRepository.findByChainIdOrNameIgnoreCase(accessibleChainId, search, pageable);
    }

    @Override
    @Transactional
    public RestaurantSetupResponse createRestaurant(RestaurantRequest req, Long chainId) {
        Long accessibleChainId = tenantAccessService.resolveAccessibleChainId(chainId);
        if (accessibleChainId == null) {
            throw new BadRequestException("Chain id is required");
        }

        RestaurantChain chain = chainRepository.findById(accessibleChainId)
                .orElseThrow(() -> new BadRequestException("Chain not found"));

        if (restaurantRepository.existsByCodeAndChain(req.getCode(), chain)) {
            throw new BadRequestException("Restaurant code already exists");
        }

        Restaurant restaurant = RestaurantMapper.toRestaurant(req, chain);
        restaurant = restaurantRepository.save(restaurant);

        return RestaurantMapper.buildResponse(chain, restaurant, null);
    }

    @Override
    public Page<RestaurantProjection> getAllRestaurants(Long chainId, Long restaurantId, Boolean isActive, String search, Pageable pageable) {
        Long accessibleChainId = tenantAccessService.resolveAccessibleChainId(chainId);
        if (!tenantAccessService.isSuperAdmin() && tenantAccessService.currentUser().getRestaurantId() != null) {
            restaurantId = tenantAccessService.currentUser().getRestaurantId();
        }
        return restaurantRepository.findRestaurants(accessibleChainId, search, restaurantId, isActive, pageable);
    }

    @Override
    public void createChainAdmin(Long chainId, String email, String phone) {
        tenantAccessService.assertSuperAdmin();
        RestaurantChain chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new BadRequestException("Chain not found"));

        userService.createChainAdmin(
                chain.getChainId(),
                email,
                phone,
                "default@123"
        );
    }

    @Override
    public void createRestaurantAdmin(Long restaurantId, String email, String phone) {
        Long accessibleRestaurantId = tenantAccessService.resolveAccessibleRestaurantId(restaurantId);
        Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(accessibleRestaurantId)
                .orElseThrow(() -> new BadRequestException("Restaurant not found"));

        userService.createRestaurantAdmin(
                restaurant.getChainId(),
                restaurant.getRestaurantId(),
                email,
                phone,
                "default@123"
        );
    }
}
