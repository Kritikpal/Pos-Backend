package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.unit.UnitMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitMasterRepository extends JpaRepository<UnitMaster, Long> {
    Optional<UnitMaster> findByCodeIgnoreCase(String code);
}
