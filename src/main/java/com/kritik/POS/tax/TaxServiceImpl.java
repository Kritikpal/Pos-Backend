package com.kritik.POS.tax;

import com.kritik.POS.exception.errors.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaxServiceImpl implements TaxService {

    private final TaxRateRepository taxRateRepository;

    public TaxServiceImpl(TaxRateRepository taxRateRepository) {
        this.taxRateRepository = taxRateRepository;
    }

    @Override
    public TaxRate saveTaxRate(TaxRequest taxRequest) {
        TaxRate taxRate = new TaxRate();
        if (taxRequest.taxId() != null) {
            taxRate = taxRateRepository.findById(taxRequest.taxId()).orElseThrow(() ->
                    new AppException(TaxErrorMessage.INVALID_TAX_ID.getMessage(), HttpStatus.CONFLICT));
        }
        taxRate.setTaxAmount(taxRequest.taxAmount());
        taxRate.setTaxName(taxRequest.taxName());
        taxRate.setActive(taxRequest.active());
        return taxRateRepository.save(taxRate);
    }

    @Override
    public boolean deleteTaxRate(Long taxId) {
        taxRateRepository.deleteById(taxId);
        return true;
    }

    @Override
    public List<TaxRate> getAllTaxRates() {
        return taxRateRepository.findAll();
    }

    @Override
    public List<TaxRate> getActiveTaxRates() {
        return taxRateRepository.findAllByIsActiveTrue();
    }
}
