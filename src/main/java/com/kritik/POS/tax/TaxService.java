package com.kritik.POS.tax;

import java.util.List;

public interface TaxService {
    TaxRate saveTaxRate(TaxRequest taxRequest);

    boolean deleteTaxRate(Long taxId);

    List<TaxRate> getAllTaxRates();
    List<TaxRate> getActiveTaxRates();
}
