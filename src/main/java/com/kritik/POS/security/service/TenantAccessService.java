package com.kritik.POS.security.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TenantAccessService {

    private static final List<Long> NOOP_RESTAURANT_FILTER = List.of(-1L);

    private final RestaurantRepository restaurantRepository;

    public SecurityUser currentUser() {
        return SecurityUtil.securityContextHolder();
    }

    public void assertSuperAdmin() {
        if (!currentUser().isSuperAdmin()) {
            throw new AppException("Only super admin can access this resource", HttpStatus.FORBIDDEN);
        }
    }

    public void assertChainAdminOrSuperAdmin() {
        SecurityUser user = currentUser();
        if (!user.isSuperAdmin() && !user.isChainAdmin()) {
            throw new AppException("Only chain admin or super admin can access this resource", HttpStatus.FORBIDDEN);
        }
    }

    public Long resolveAccessibleChainId(@Nullable Long requestedChainId) {
        SecurityUser user = currentUser();
        if (user.isSuperAdmin()) {
            return requestedChainId;
        }

        Long chainId = requireChainId(user);
        if (requestedChainId != null && !Objects.equals(requestedChainId, chainId)) {
            throw new AppException("Chain access denied", HttpStatus.FORBIDDEN);
        }
        return chainId;
    }

    public Long resolveAccessibleRestaurantId(@Nullable Long requestedRestaurantId) {
        SecurityUser user = currentUser();
        if (user.isSuperAdmin()) {
            return requestedRestaurantId;
        }
        if (user.isChainAdmin()) {
            if (requestedRestaurantId == null) {
                throw new AppException("Restaurant id is required for this operation", HttpStatus.BAD_REQUEST);
            }
            validateRestaurantBelongsToChain(requestedRestaurantId, requireChainId(user));
            return requestedRestaurantId;
        }

        Long restaurantId = requireRestaurantId(user);
        if (requestedRestaurantId != null && !Objects.equals(requestedRestaurantId, restaurantId)) {
            throw new AppException("Restaurant access denied", HttpStatus.FORBIDDEN);
        }
        return restaurantId;
    }

    public List<Long> resolveAccessibleRestaurantIds(@Nullable Long requestedChainId, @Nullable Long requestedRestaurantId) {
        SecurityUser user = currentUser();
        if (user.isSuperAdmin()) {
            if (requestedRestaurantId != null) {
                return List.of(requestedRestaurantId);
            }
            if (requestedChainId != null) {
                return restaurantRepository.findActiveRestaurantIdsByChainId(requestedChainId);
            }
            return List.of();
        }

        Long chainId = requireChainId(user);
        if (requestedChainId != null && !Objects.equals(requestedChainId, chainId)) {
            throw new AppException("Chain access denied", HttpStatus.FORBIDDEN);
        }

        if (user.isChainAdmin()) {
            if (requestedRestaurantId != null) {
                validateRestaurantBelongsToChain(requestedRestaurantId, chainId);
                return List.of(requestedRestaurantId);
            }
            return restaurantRepository.findActiveRestaurantIdsByChainId(chainId);
        }

        Long restaurantId = requireRestaurantId(user);
        if (requestedRestaurantId != null && !Objects.equals(requestedRestaurantId, restaurantId)) {
            throw new AppException("Restaurant access denied", HttpStatus.FORBIDDEN);
        }
        return List.of(restaurantId);
    }

    public boolean isSuperAdmin() {
        return currentUser().isSuperAdmin();
    }

    public List<Long> queryRestaurantIds(List<Long> accessibleRestaurantIds) {
        return accessibleRestaurantIds == null || accessibleRestaurantIds.isEmpty()
                ? NOOP_RESTAURANT_FILTER
                : accessibleRestaurantIds;
    }

    private void validateRestaurantBelongsToChain(Long restaurantId, Long chainId) {
        boolean allowed = restaurantRepository.existsByRestaurantIdAndChainIdAndIsDeletedFalse(restaurantId, chainId);
        if (!allowed) {
            throw new AppException("Restaurant access denied", HttpStatus.FORBIDDEN);
        }
    }

    private Long requireChainId(SecurityUser user) {
        if (user.getChainId() == null) {
            throw new AppException("Chain id is missing from the authenticated user", HttpStatus.FORBIDDEN);
        }
        return user.getChainId();
    }

    private Long requireRestaurantId(SecurityUser user) {
        if (user.getRestaurantId() == null) {
            throw new AppException("Restaurant id is missing from the authenticated user", HttpStatus.FORBIDDEN);
        }
        return user.getRestaurantId();
    }
}
