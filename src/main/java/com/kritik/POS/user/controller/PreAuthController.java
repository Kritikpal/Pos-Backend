package com.kritik.POS.user.controller;


import com.kritik.POS.common.enums.ResponseCode;
import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.user.DAO.User;
import com.kritik.POS.user.model.request.LoginRequest;
import com.kritik.POS.user.model.request.SignUpRequest;
import com.kritik.POS.user.model.response.LoginResponse;
import com.kritik.POS.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kritik.POS.user.route.UserRoute.LOGIN;
import static com.kritik.POS.user.route.UserRoute.SIGN_UP;

@RestController
@RequestMapping("/pre-auth")
@Tag(name = "Authentication",description = "Login, sign up and forgot password")
public class PreAuthController {

    private final UserService userService;

    public PreAuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(SIGN_UP)
    public ResponseEntity<ApiResponse<User>> signUp(@RequestBody @Valid SignUpRequest signUpRequest) throws AppException {
        User user = userService.signUp(signUpRequest);
        ApiResponse<User> newUser = new ApiResponse<>(user, ResponseCode.SUCCESS, "user created successfully");
        return  ResponseEntity.ok(newUser);
    }

    @PostMapping(LOGIN)
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest loginRequest) throws AppException {
        LoginResponse loginresponse = userService.login(loginRequest);
        ApiResponse<LoginResponse> loginResponse = new ApiResponse<>(loginresponse, ResponseCode.SUCCESS, "user created successfully");
        return  ResponseEntity.ok(loginResponse);
    }


}
