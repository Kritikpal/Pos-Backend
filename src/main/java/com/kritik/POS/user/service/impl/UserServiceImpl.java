package com.kritik.POS.user.service.impl;

import com.kritik.POS.exception.errors.BadRequestException;
import com.kritik.POS.user.entity.Role;
import com.kritik.POS.user.entity.User;
import com.kritik.POS.user.repository.RoleRepository;
import com.kritik.POS.user.repository.UserRepository;
import com.kritik.POS.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void createRestaurantAdmin(Long chainId, Long restaurantId, String email, String phone, String password) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("User already exists");
        }

        Role role = roleRepository.findByRoleName("RESTAURANT_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));

        // only IDs (important for decoupling)
        user.setChainId(chainId);
        user.setRestaurantId(restaurantId);

        user.setRoles(Set.of(role));

        userRepository.save(user);
    }

    @Override
    public void createChainAdmin(Long chainId, String email, String phone, String password) {

        Role role = roleRepository.findByRoleName("CHAIN_ADMIN")
                .orElseThrow(() -> new RuntimeException("Role not found"));

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
                .orElseThrow(() -> new RuntimeException("Role not found"));

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
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRestaurantId(restaurantId);
        user.setRoles(Set.of(role));

        userRepository.save(user);
    }
}