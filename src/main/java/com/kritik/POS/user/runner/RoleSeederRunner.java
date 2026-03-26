package com.kritik.POS.user.runner;

import com.kritik.POS.user.service.impl.RoleSeederService;
import com.kritik.POS.user.service.impl.SuperAdminSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleSeederRunner implements CommandLineRunner {

    private final RoleSeederService roleSeederService;
    private final SuperAdminSeederService superAdminSeederService;

    @Override
    public void run(String... args) {
        roleSeederService.seedRoles();
        superAdminSeederService.seedSuperAdmin();
    }
}