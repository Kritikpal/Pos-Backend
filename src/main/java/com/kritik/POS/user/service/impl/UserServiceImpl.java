package com.kritik.POS.user.service.impl;

import com.kritik.POS.exception.errors.BadRequestException;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.user.entity.Role;
import com.kritik.POS.user.entity.User;
import com.kritik.POS.user.repository.RoleRepository;
import com.kritik.POS.user.repository.UserRepository;
import com.kritik.POS.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestaurantRepository restaurantRepository;

    @Override
    public void createRestaurantAdmin(Long chainId, Long restaurantId, String email, String phone, String password) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("User already exists");
        }

        Role role = roleRepository.findByRoleName("RESTAURANT_ADMIN")
                .orElseThrow(() -> new AppException("RESTAURANT_ADMIN role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setChainId(chainId);
        user.setRestaurantId(restaurantId);
        user.setRoles(Set.of(role));

        userRepository.save(user);
    }

    @Override
    public void createChainAdmin(Long chainId, String email, String phone, String password) {

        Role role = roleRepository.findByRoleName("CHAIN_ADMIN")
                .orElseThrow(() -> new AppException("CHAIN_ADMIN role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setChainId(chainId);
        user.setRoles(Set.of(role));

        userRepository.save(user);
    }

    @Override
    public void createSuperAdmin(String email, String phone, String password) {

        Role role = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseThrow(() -> new AppException("SUPER_ADMIN role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Set.of(role));

        userRepository.save(user);
    }


    @Override
    public void validateUserNotExists(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("User already exists");
        }
    }

    @Override
    public void createStaff(Long restaurantId, String email, String phone, String password) {
        Role role = roleRepository.findByRoleName("STAFF")
                .orElseThrow(() -> new AppException("STAFF role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
                .orElseThrow(() -> new BadRequestException("Restaurant not found"));

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRestaurantId(restaurantId);
        user.setChainId(restaurant.getChainId());
        user.setRoles(Set.of(role));

        userRepository.save(user);
    }
}
