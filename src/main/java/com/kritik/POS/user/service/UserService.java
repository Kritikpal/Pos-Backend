package com.kritik.POS.user.service;

import com.kritik.POS.exception.errors.AppException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public interface UserService {

    void createSuperAdmin(String email, String phone, String password) throws AppException;

    void createChainAdmin(Long chainId, String email, String phone, String password) throws AppException;

    void createRestaurantAdmin(Long chainId, Long restaurantId, String email, String phone, String password) throws AppException;

    void createStaff(Long restaurantId, String email, String phone, String password) throws AppException;

    void validateUserNotExists( String adminEmail);
}