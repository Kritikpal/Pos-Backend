package com.kritik.POS.user.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.user.model.request.UserCreateRequest;
import com.kritik.POS.user.model.request.UserUpdateRequest;
import com.kritik.POS.user.model.response.UserProjection;
import com.kritik.POS.user.model.response.UserResponse;
import com.kritik.POS.user.service.UserService;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@Validated
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @Tag(name = SwaggerTags.ADMIN)
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @RequestBody @Valid UserCreateRequest request
    ) throws AppException {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.SUCCESS(response, "User created and email sent"));
    }

    @Tag(name = SwaggerTags.ADMIN)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserProjection>>> listUsers(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) throws AppException {
        Page<UserProjection> result = userService.getUsers(chainId, restaurantId, search, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.SUCCESS(result));
    }

    @Tag(name = SwaggerTags.ADMIN)
    @GetMapping("/chain-admins")
    public ResponseEntity<ApiResponse<Page<UserProjection>>> listChainAdmins(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) throws AppException {
        Page<UserProjection> result = userService.getChainAdmins(chainId, search, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.SUCCESS(result));
    }

    @Tag(name = SwaggerTags.ADMIN)
    @GetMapping("/store-admins")
    public ResponseEntity<ApiResponse<Page<UserProjection>>> listStoreAdmins(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) throws AppException {
        Page<UserProjection> result = userService.getRestaurantAdmins(chainId, restaurantId, search, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.SUCCESS(result));
    }

    @Tag(name = SwaggerTags.ADMIN)
    @GetMapping("/staffs")
    public ResponseEntity<ApiResponse<Page<UserProjection>>> listStaffs(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) throws AppException {
        Page<UserProjection> result = userService.getStaffs(chainId, restaurantId, search, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.SUCCESS(result));
    }

    @Tag(name = SwaggerTags.ADMIN)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) throws AppException {
        return ResponseEntity.ok(ApiResponse.SUCCESS(userService.getUser(id)));
    }

    @Tag(name = SwaggerTags.ADMIN)
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UserUpdateRequest request
    ) throws AppException {
        return ResponseEntity.ok(ApiResponse.SUCCESS(userService.updateUser(id, request), "User updated"));
    }

    @Tag(name = SwaggerTags.ADMIN)
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Boolean>> resetPassword(
            @RequestBody @Valid com.kritik.POS.user.model.request.ResetPasswordRequest request
    ) throws AppException {
        boolean result = userService.resetPassword(request.getUserId(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.SUCCESS(result, "Password reset successfully"));
    }
}
