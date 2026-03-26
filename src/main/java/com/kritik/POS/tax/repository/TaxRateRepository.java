package com.kritik.POS.tax.repository;

import com.kritik.POS.tax.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxRateRepository extends JpaRepository<TaxRate,Long> {

    List<TaxRate> findAllByIsActiveTrue();

}
