package com.kritik.POS;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithVerificationTest {

    @Test
    void verifiesModuleStructure() {
        ApplicationModules.of(PosApplication.class).verify();
    }
}
