package com.kritik.POS.restaurant.service.Impl;

import com.kritik.POS.exception.errors.BadRequestException;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.restaurant.models.request.RestaurantChainRequest;
import com.kritik.POS.restaurant.models.request.RestaurantRequest;
import com.kritik.POS.restaurant.repository.RestaurantChainRepository;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantChainRepository chainRepository;

    @Mock
    private UserService userService;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private SuperAdminServiceImpl superAdminService;

    @Test
    void updateRestaurantUsesManageableRestaurantIdAndPersistsChanges() {
        RestaurantChain chain = new RestaurantChain();
        chain.setChainId(11L);
        chain.setName("North Kitchens");

        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(21L);
        restaurant.setChain(chain);
        restaurant.setChainId(11L);
        restaurant.setName("Old Name");
        restaurant.setCode("OLD001");
        restaurant.setCity("Delhi");
        restaurant.setCurrency("INR");
        restaurant.setTimezone("Asia/Kolkata");
        restaurant.setActive(true);

        RestaurantRequest request = new RestaurantRequest();
        request.setRestaurantName("New Name");
        request.setCode("NEW001");
        request.setCity("Noida");
        request.setRestaurantPhone("9876543210");
        request.setRestaurantEmail("admin@example.com");
        request.setIsActive(false);

        when(tenantAccessService.resolveManageableRestaurantId(21L)).thenReturn(21L);
        when(restaurantRepository.findDetailByRestaurantId(21L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.existsByCodeAndChainAndRestaurantIdNot("NEW001", chain, 21L)).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = superAdminService.updateRestaurant(21L, request);

        assertEquals(21L, response.restaurantId());
        assertEquals("New Name", response.restaurantName());
        assertEquals("NEW001", response.code());
        assertEquals("Noida", response.city());
        assertEquals("North Kitchens", response.chainName());
        verify(tenantAccessService).resolveManageableRestaurantId(21L);
    }

    @Test
    void updateChainRejectsDuplicateName() {
        RestaurantChain chain = new RestaurantChain();
        chain.setChainId(7L);
        chain.setName("Existing");
        chain.setActive(true);

        RestaurantChainRequest request = new RestaurantChainRequest();
        request.setName("Duplicate");

        when(tenantAccessService.resolveManageableChainId(7L)).thenReturn(7L);
        when(chainRepository.findByChainIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(chain));
        when(chainRepository.existsByNameIgnoreCaseAndChainIdNot("Duplicate", 7L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> superAdminService.updateChain(7L, request));

        verify(chainRepository, never()).save(any(RestaurantChain.class));
    }
}
