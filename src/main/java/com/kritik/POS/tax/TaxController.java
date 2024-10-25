package com.kritik.POS.tax;

import com.kritik.POS.common.model.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TaxController {
    private final TaxService taxService;

    public TaxController(TaxService taxService) {
        this.taxService = taxService;
    }

    @GetMapping(TaxRoute.GET_ALL_TAX_LIST)
    public ApiResponse<List<TaxRate>> getAllTaxRates() {
        return ApiResponse.SUCCESS(taxService.getAllTaxRates());
    }

    @PostMapping(TaxRoute.SAVE_TAX)
    public ApiResponse<TaxRate> saveTaxRate(@RequestBody @Valid TaxRequest taxRequest) {
        return ApiResponse.SUCCESS(taxService.saveTaxRate(taxRequest));
    }

    @DeleteMapping(TaxRoute.DELETE_TAX)
    public ApiResponse<Boolean> deleteTaxRate(@PathVariable(name = "id") Long taxId) {
        return ApiResponse.SUCCESS(taxService.deleteTaxRate(taxId));
    }

}
