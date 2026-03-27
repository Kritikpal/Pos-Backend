package com.kritik.POS.restaurant.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.models.request.RestaurantRequest;
import com.kritik.POS.restaurant.models.request.RestaurantSetupRequest;
import com.kritik.POS.restaurant.models.response.RestaurantChainInfo;
import com.kritik.POS.restaurant.models.response.RestaurantProjection;
import com.kritik.POS.restaurant.models.response.RestaurantSetupResponse;
import com.kritik.POS.restaurant.route.SuperAdminRoute;
import com.kritik.POS.restaurant.service.SuperAdminService;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @Tag(name = SwaggerTags.SUPER_ADMIN)
    @PostMapping(SuperAdminRoute.SETUP_RESTAURANT)
    public ResponseEntity<ApiResponse<RestaurantSetupResponse>> setupRestaurant(
            @RequestBody @Valid RestaurantSetupRequest request
    ) throws AppException {
        RestaurantSetupResponse response = superAdminService.createRestaurantSetup(request);
        return ResponseEntity.ok(ApiResponse.SUCCESS(response, "Restaurant setup completed"));
    }

    @Tag(name = SwaggerTags.CHAIN)
    @PostMapping(SuperAdminRoute.CREATE_CHAIN)
    public ResponseEntity<ApiResponse<Long>> createChain(
            @RequestParam @NotBlank(message = "name is required") String name
    ) throws AppException {
        Long chainId = superAdminService.createChain(name);
        return ResponseEntity.ok(ApiResponse.SUCCESS(chainId, "Chain created"));
    }

    @Tag(name = SwaggerTags.CHAIN)
    @GetMapping(SuperAdminRoute.GET_ALL_CHAINS)
    public ResponseEntity<ApiResponse<Page<RestaurantChainInfo>>> getAllChains(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) throws AppException {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                superAdminService.getAllChains(chainId, search, PageRequest.of(page, size))
        ));
    }

    @Tag(name = SwaggerTags.RESTAURANT)
    @PostMapping(SuperAdminRoute.CREATE_RESTAURANT)
    public ResponseEntity<ApiResponse<RestaurantSetupResponse>> createRestaurant(
            @RequestBody @Valid RestaurantRequest request,
            @RequestParam(required = false) Long chainId
    ) throws AppException {
        return ResponseEntity.ok(
                ApiResponse.SUCCESS(superAdminService.createRestaurant(request, chainId), "Restaurant created")
        );
    }

    @Tag(name = SwaggerTags.RESTAURANT)
    @GetMapping(SuperAdminRoute.GET_ALL_RESTAURANTS)
    public ResponseEntity<ApiResponse<Page<RestaurantProjection>>> getAllRestaurants(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) throws AppException {
        return ResponseEntity.ok(
                ApiResponse.SUCCESS(
                        superAdminService.getAllRestaurants(chainId, restaurantId, isActive, search, PageRequest.of(page, size))
                )
        );
    }

    @Tag(name = SwaggerTags.ADMIN)
    @PostMapping(SuperAdminRoute.CREATE_CHAIN_ADMIN)
    public ResponseEntity<ApiResponse<String>> createChainAdmin(
            @PathVariable Long chainId,
            @RequestParam @Email(message = "valid email is required") String email,
            @RequestParam @Pattern(regexp = "^[0-9]{10}$", message = "valid 10 digit phone is required") String phone
    ) throws AppException {
        superAdminService.createChainAdmin(chainId, email, phone);
        return ResponseEntity.ok(ApiResponse.SUCCESS("Chain admin created"));
    }

    @Tag(name = SwaggerTags.ADMIN)
    @PostMapping(SuperAdminRoute.CREATE_RESTAURANT_ADMIN)
    public ResponseEntity<ApiResponse<String>> createRestaurantAdmin(
            @PathVariable Long restaurantId,
            @RequestParam @Email(message = "valid email is required") String email,
            @RequestParam @Pattern(regexp = "^[0-9]{10}$", message = "valid 10 digit phone is required") String phone
    ) throws AppException {
        superAdminService.createRestaurantAdmin(restaurantId, email, phone);
        return ResponseEntity.ok(ApiResponse.SUCCESS("Restaurant admin created"));
    }
}
