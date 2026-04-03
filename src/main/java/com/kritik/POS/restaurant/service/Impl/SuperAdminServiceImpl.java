package com.kritik.POS.restaurant.service.Impl;

import com.kritik.POS.exception.errors.BadRequestException;
import com.kritik.POS.restaurant.dto.RestaurantChainResponseDto;
import com.kritik.POS.restaurant.dto.RestaurantDetailResponseDto;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.restaurant.models.request.RestaurantChainRequest;
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
import org.springframework.util.StringUtils;

import java.util.UUID;

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
        tenantAccessService.assertChainAdminOrSuperAdmin();
        Long accessibleChainId = tenantAccessService.resolveAccessibleChainId(chainId);
        return chainRepository.findByChainIdOrNameIgnoreCase(accessibleChainId, search, pageable);
    }

    @Override
    public RestaurantChainResponseDto getChain(Long chainId) {
        tenantAccessService.assertChainAdminOrSuperAdmin();
        Long accessibleChainId = tenantAccessService.resolveAccessibleChainId(chainId);
        return toChainResponse(getChainEntity(accessibleChainId));
    }

    @Override
    @Transactional
    public RestaurantChainResponseDto updateChain(Long chainId, RestaurantChainRequest request) {
        Long manageableChainId = tenantAccessService.resolveManageableChainId(chainId);
        RestaurantChain chain = getChainEntity(manageableChainId);

        if (chainRepository.existsByNameIgnoreCaseAndChainIdNot(request.getName(), manageableChainId)) {
            throw new BadRequestException("Chain already exists");
        }

        chain.setName(request.getName().trim());
        chain.setDescription(request.getDescription());
        chain.setLogoUrl(request.getLogoUrl());
        chain.setEmail(request.getEmail());
        chain.setPhoneNumber(request.getPhoneNumber());
        chain.setGstNumber(request.getGstNumber());
        if (request.getIsActive() != null) {
            chain.setActive(request.getIsActive());
        }

        return toChainResponse(chainRepository.save(chain));
    }

    @Override
    @Transactional
    public RestaurantSetupResponse createRestaurant(RestaurantRequest req, Long chainId) {
        tenantAccessService.assertChainAdminOrSuperAdmin();
        Long accessibleChainId = tenantAccessService.resolveManageableChainId(chainId);
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
    public RestaurantDetailResponseDto getRestaurant(Long restaurantId) {
        Long accessibleRestaurantId = tenantAccessService.resolveAccessibleRestaurantId(restaurantId);
        return toRestaurantResponse(getRestaurantEntity(accessibleRestaurantId));
    }

    @Override
    @Transactional
    public RestaurantDetailResponseDto updateRestaurant(Long restaurantId, RestaurantRequest request) {
        Long manageableRestaurantId = tenantAccessService.resolveManageableRestaurantId(restaurantId);
        Restaurant restaurant = getRestaurantEntity(manageableRestaurantId);
        String nextCode = resolveRestaurantCode(request, restaurant.getCode());

        if (restaurantRepository.existsByCodeAndChainAndRestaurantIdNot(nextCode, restaurant.getChain(), manageableRestaurantId)) {
            throw new BadRequestException("Restaurant code already exists");
        }

        restaurant.setName(request.getRestaurantName().trim());
        restaurant.setCode(nextCode);
        restaurant.setAddressLine1(request.getAddressLine1());
        restaurant.setAddressLine2(request.getAddressLine2());
        restaurant.setCity(request.getCity());
        restaurant.setState(request.getState());
        restaurant.setCountry(request.getCountry());
        restaurant.setPincode(request.getPincode());
        restaurant.setPhoneNumber(request.getRestaurantPhone());
        restaurant.setEmail(request.getRestaurantEmail());
        restaurant.setGstNumber(request.getRestaurantGstNumber());
        if (request.getIsActive() != null) {
            restaurant.setActive(request.getIsActive());
        }

        return toRestaurantResponse(restaurantRepository.save(restaurant));
    }

    @Override
    public void createChainAdmin(Long chainId, String email, String phone) {
        tenantAccessService.assertSuperAdmin();
        RestaurantChain chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new BadRequestException("Chain not found"));

        String password = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        userService.createChainAdmin(
                chain.getChainId(),
                email,
                phone,
                password
        );
    }

    @Override
    public void createRestaurantAdmin(Long restaurantId, String email, String phone) {
        tenantAccessService.assertChainAdminOrSuperAdmin();
        Long accessibleRestaurantId = tenantAccessService.resolveAccessibleRestaurantId(restaurantId);
        Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(accessibleRestaurantId)
                .orElseThrow(() -> new BadRequestException("Restaurant not found"));

        String password = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        userService.createRestaurantAdmin(
                restaurant.getChainId(),
                restaurant.getRestaurantId(),
                email,
                phone,
                password
        );
    }

    private RestaurantChain getChainEntity(Long chainId) {
        return chainRepository.findByChainIdAndIsDeletedFalse(chainId)
                .orElseThrow(() -> new BadRequestException("Chain not found"));
    }

    private Restaurant getRestaurantEntity(Long restaurantId) {
        return restaurantRepository.findDetailByRestaurantId(restaurantId)
                .orElseThrow(() -> new BadRequestException("Restaurant not found"));
    }

    private RestaurantChainResponseDto toChainResponse(RestaurantChain chain) {
        return new RestaurantChainResponseDto(
                chain.getChainId(),
                chain.getName(),
                chain.getDescription(),
                chain.getLogoUrl(),
                chain.getEmail(),
                chain.getPhoneNumber(),
                chain.getGstNumber(),
                chain.isActive()
        );
    }

    private RestaurantDetailResponseDto toRestaurantResponse(Restaurant restaurant) {
        return new RestaurantDetailResponseDto(
                restaurant.getChainId(),
                restaurant.getChain().getName(),
                restaurant.getRestaurantId(),
                restaurant.getName(),
                restaurant.getCode(),
                restaurant.getAddressLine1(),
                restaurant.getAddressLine2(),
                restaurant.getCity(),
                restaurant.getState(),
                restaurant.getCountry(),
                restaurant.getPincode(),
                restaurant.getPhoneNumber(),
                restaurant.getEmail(),
                restaurant.getGstNumber(),
                restaurant.getCurrency(),
                restaurant.getTimezone(),
                restaurant.isActive()
        );
    }

    private String resolveRestaurantCode(RestaurantRequest request, String fallbackCode) {
        if (StringUtils.hasText(request.getCode())) {
            return request.getCode().trim();
        }
        return fallbackCode;
    }
}
