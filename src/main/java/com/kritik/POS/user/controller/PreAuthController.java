package com.kritik.POS.user.controller;

import static com.kritik.POS.user.route.UserRoute.LOGIN;
import static com.kritik.POS.user.route.UserRoute.LOGOUT;
import static com.kritik.POS.user.route.UserRoute.REFRESH_TOKEN;

import com.kritik.POS.common.enums.ResponseCode;
import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.user.model.request.LoginRequest;
import com.kritik.POS.user.model.request.LogoutRequest;
import com.kritik.POS.user.model.request.RefreshTokenRequest;
import com.kritik.POS.user.model.response.LoginResponse;
import com.kritik.POS.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for login, token rotation, and logout")
public class PreAuthController {

    private final AuthService authService;

    public PreAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(LOGIN)
    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns an access token plus refresh token.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest loginRequest) throws AppException {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(new ApiResponse<>(response, ResponseCode.SUCCESS, "Login successful"));
    }

    @PostMapping(REFRESH_TOKEN)
    @Operation(summary = "Refresh access token", description = "Rotates the supplied refresh token and issues a new access and refresh token pair.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody @Valid RefreshTokenRequest request) throws AppException {
        LoginResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(new ApiResponse<>(response, ResponseCode.SUCCESS, "Token refreshed successfully"));
    }

    @PostMapping(LOGOUT)
    @Operation(summary = "Logout session", description = "Revokes the current session using the bearer token or refresh token so protected APIs reject it immediately.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) LogoutRequest request
    ) throws AppException {
        authService.logout(authorizationHeader, request != null ? request.refreshToken() : null);
        return ResponseEntity.ok(new ApiResponse<>(null, ResponseCode.SUCCESS, "Logout successful"));
    }
}
