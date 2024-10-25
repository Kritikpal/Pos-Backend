package com.kritik.POS.user.service.impl;


import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.security.util.JwtUtil;
import com.kritik.POS.user.DAO.User;
import com.kritik.POS.user.model.request.LoginRequest;
import com.kritik.POS.user.model.request.SignUpRequest;
import com.kritik.POS.user.model.response.LoginResponse;
import com.kritik.POS.user.repository.UserRepository;
import com.kritik.POS.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public User signUp(SignUpRequest signUpRequest) throws AppException {
        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(signUpRequest.getPassword());
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        if (signUpRequest.getUserRole() != null) {
            user.setUserRole(signUpRequest.getUserRole());
        }
        try {
            return userRepository.save(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AppException("Unable to store the value in db", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) throws AppException {
        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow(() -> new AppException("This email is not registered", HttpStatus.BAD_REQUEST));

        if (!user.getPassword().equals(loginRequest.password())) {
            throw new AppException("Username or password is not matching", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getUserRole().name());
        claims.put("phone", user.getPhoneNumber());
        claims.put("restaurant", loginRequest.restaurantId());
        String accessToken = new JwtUtil().generateToken(user.getEmail(), claims, System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000);
        return new LoginResponse(accessToken);
    }

    @Override
    public User getUserByUserName(String email) throws AppException {
        return userRepository.findByEmail(email).orElseThrow(() -> new AppException("User not found", HttpStatus.BAD_REQUEST));
    }
}
