package com.kritik.POS.user.controller;


import com.kritik.POS.common.enums.ResponseCode;
import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.user.entity.User;
import com.kritik.POS.user.model.request.LoginRequest;
import com.kritik.POS.user.model.request.SignUpRequest;
import com.kritik.POS.user.model.response.LoginResponse;
import com.kritik.POS.user.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kritik.POS.user.route.UserRoute.LOGIN;
import static com.kritik.POS.user.route.UserRoute.REFRESH_TOKEN;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login, sign up and forgot password")
public class PreAuthController {

    private final AuthService authService;

    public PreAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(LOGIN)
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest loginRequest) throws AppException {
        LoginResponse loginresponse = authService.login(loginRequest);
        ApiResponse<LoginResponse> loginResponse = new ApiResponse<>(loginresponse, ResponseCode.SUCCESS, "user created successfully");
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping(REFRESH_TOKEN)
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody @Valid LoginResponse request) throws AppException {
        LoginResponse res = authService.refreshToken(request.getRefreshToken());
        ApiResponse<LoginResponse> loginResponse = new ApiResponse<>(res, ResponseCode.SUCCESS, "user created successfully");
        return ResponseEntity.ok(loginResponse);
    }


}
