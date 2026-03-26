package com.kritik.POS.user.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.user.entity.User;
import com.kritik.POS.user.model.request.LoginRequest;
import com.kritik.POS.user.model.request.SignUpRequest;
import com.kritik.POS.user.model.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest loginRequest) throws  AppException;
    LoginResponse refreshToken(String refreshToken) throws  AppException;

    User getUserByUserName(String userId) throws AppException;
}
