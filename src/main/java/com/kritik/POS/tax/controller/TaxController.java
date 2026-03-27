package com.kritik.POS.tax.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.tax.dto.TaxRateResponseDto;
import com.kritik.POS.tax.dto.TaxRequest;
import com.kritik.POS.tax.entity.TaxRate;
import com.kritik.POS.tax.route.TaxRoute;
import com.kritik.POS.tax.service.TaxService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class TaxController {
    private final TaxService taxService;

    @GetMapping(TaxRoute.GET_ALL_TAX_LIST)
    public ApiResponse<List<TaxRate>> getAllTaxRates() {
        return ApiResponse.SUCCESS(taxService.getAllTaxRates());
    }

    @GetMapping(TaxRoute.GET_TAX_PAGE)
    public ApiResponse<PageResponse<TaxRateResponseDto>> getTaxRatePage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ApiResponse.SUCCESS(taxService.getTaxRatePage(chainId, restaurantId, isActive, search, page, size));
    }

    @PostMapping(TaxRoute.SAVE_TAX)
    public ApiResponse<TaxRate> saveTaxRate(@RequestBody @Valid TaxRequest taxRequest) {
        return ApiResponse.SUCCESS(taxService.saveTaxRate(taxRequest), "Tax rate saved successfully");
    }

    @DeleteMapping(TaxRoute.DELETE_TAX)
    public ApiResponse<Boolean> deleteTaxRate(@PathVariable(name = "id") Long taxId) {
        return ApiResponse.SUCCESS(taxService.deleteTaxRate(taxId), "Tax rate deleted successfully");
    }
}
