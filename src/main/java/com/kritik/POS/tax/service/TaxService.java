package com.kritik.POS.tax.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.tax.dto.TaxRateResponseDto;
import com.kritik.POS.tax.dto.TaxRequest;
import com.kritik.POS.tax.entity.TaxRate;

import java.util.List;

public interface TaxService {
    TaxRate saveTaxRate(TaxRequest taxRequest);

    boolean deleteTaxRate(Long taxId);

    List<TaxRate> getAllTaxRates();
    List<TaxRate> getActiveTaxRates();
    PageResponse<TaxRateResponseDto> getTaxRatePage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize);
}
