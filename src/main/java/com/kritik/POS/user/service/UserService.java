package com.kritik.POS.user.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.user.model.request.UserCreateRequest;
import com.kritik.POS.user.model.request.UserUpdateRequest;
import com.kritik.POS.user.model.response.UserProjection;
import com.kritik.POS.user.model.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    void createSuperAdmin(String email, String phone, String password) throws AppException;

    void createChainAdmin(Long chainId, String email, String phone, String password) throws AppException;

    void createRestaurantAdmin(Long chainId, Long restaurantId, String email, String phone, String password) throws AppException;

    void createStaff(Long restaurantId, String email, String phone, String password) throws AppException;

    void validateUserNotExists(String adminEmail);

    UserResponse createUser(UserCreateRequest request) throws AppException;

    UserResponse updateUser(Long userId, UserUpdateRequest request) throws AppException;

    UserResponse getUser(Long userId) throws AppException;

    Page<UserProjection> getUsers(Long chainId, Long restaurantId, String search, Pageable pageable) throws AppException;

    Page<UserProjection> getChainAdmins(Long chainId, String search, Pageable pageable) throws AppException;

    Page<UserProjection> getRestaurantAdmins(Long chainId, Long restaurantId, String search, Pageable pageable) throws AppException;

    Page<UserProjection> getStaffs(Long chainId, Long restaurantId, String search, Pageable pageable) throws AppException;

    boolean resetPassword(Long userId, String newPassword) throws AppException;

    boolean sendPasswordResetEmail(String email) throws AppException;

    boolean changePasswordFromResetToken(String token, String newPassword) throws AppException;
}