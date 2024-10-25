package com.kritik.POS.user.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.user.DAO.User;
import com.kritik.POS.user.model.request.LoginRequest;
import com.kritik.POS.user.model.request.SignUpRequest;
import com.kritik.POS.user.model.response.LoginResponse;

public interface UserService  {
    User signUp(SignUpRequest signUpRequest) throws AppException;

    LoginResponse login(LoginRequest loginRequest) throws  AppException;

    User getUserByUserName(String userId) throws AppException;
}
