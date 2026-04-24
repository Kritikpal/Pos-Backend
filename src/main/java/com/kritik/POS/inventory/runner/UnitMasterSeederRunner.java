package com.kritik.POS.inventory.runner;

import com.kritik.POS.inventory.service.Impl.UnitMasterSeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnitMasterSeederRunner implements CommandLineRunner {

    private final UnitMasterSeederService unitMasterSeederService;

    @Override
    public void run(String... args) {
        unitMasterSeederService.seedUnitMasters();
    }
}
