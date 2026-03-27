package com.kritik.POS.user.service.impl;

import com.kritik.POS.user.entity.Role;
import com.kritik.POS.user.entity.User;
import com.kritik.POS.user.model.enums.UserRole;
import com.kritik.POS.user.repository.RoleRepository;
import com.kritik.POS.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SuperAdminSeederService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.superadmin.email}")
    private String adminEmail;

    @Value("${app.superadmin.password}")
    private String adminPassword;

    @Transactional
    public void seedSuperAdmin() {

        // 1. Check if user already exists
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        // 2. Get SUPER_ADMIN role
        Role role = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not found"));

        // 3. Create user
        User user = new User();
        user.setEmail(adminEmail);
        user.setPassword(passwordEncoder.encode(adminPassword));
        user.setPhoneNumber("9999999999"); // temp or config

        // if using Role table:
         user.setRoles(Set.of(role));

        userRepository.save(user);
    }
}
