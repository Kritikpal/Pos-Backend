package com.kritik.POS.user.service.impl;

import com.kritik.POS.user.entity.Role;
import com.kritik.POS.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleSeederService {

    private final RoleRepository roleRepository;

    private static final List<String> DEFAULT_ROLES = List.of(
            "SUPER_ADMIN",
            "CHAIN_ADMIN",
            "RESTAURANT_ADMIN",
            "STAFF"
    );

    @Transactional
    public void seedRoles() {
        for (String roleName : DEFAULT_ROLES) {
            roleRepository.findByRoleName(roleName)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setRoleName(roleName);
                        return roleRepository.save(role);
                    });
        }
    }
}