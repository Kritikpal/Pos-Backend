package com.kritik.POS.tax.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.tax.dto.TaxClassRequest;
import com.kritik.POS.tax.dto.TaxClassResponseDto;
import com.kritik.POS.tax.dto.TaxDefinitionRequest;
import com.kritik.POS.tax.dto.TaxDefinitionResponseDto;
import com.kritik.POS.tax.dto.TaxRegistrationRequest;
import com.kritik.POS.tax.dto.TaxRegistrationResponseDto;
import com.kritik.POS.tax.dto.TaxRuleRequest;
import com.kritik.POS.tax.dto.TaxRuleResponseDto;
import com.kritik.POS.tax.route.TaxRoute;
import com.kritik.POS.tax.service.TaxService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

    @GetMapping(TaxRoute.GET_TAX_CLASSES)
    public ApiResponse<PageResponse<TaxClassResponseDto>> getTaxClassPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size
    ) {
        return ApiResponse.SUCCESS(taxService.getTaxClassPage(chainId, restaurantId, isActive, search, page, size));
    }

    @PostMapping(TaxRoute.SAVE_TAX_CLASS)
    public ApiResponse<TaxClassResponseDto> saveTaxClass(@RequestBody @Valid TaxClassRequest request) {
        return ApiResponse.SUCCESS(taxService.saveTaxClass(request), "Tax class saved successfully");
    }

    @DeleteMapping(TaxRoute.DELETE_TAX_CLASS)
    public ApiResponse<Boolean> deleteTaxClass(@PathVariable Long id) {
        return ApiResponse.SUCCESS(taxService.deleteTaxClass(id), "Tax class deleted successfully");
    }

    @GetMapping(TaxRoute.GET_TAX_DEFINITIONS)
    public ApiResponse<PageResponse<TaxDefinitionResponseDto>> getTaxDefinitionPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size
    ) {
        return ApiResponse.SUCCESS(taxService.getTaxDefinitionPage(chainId, restaurantId, isActive, search, page, size));
    }

    @PostMapping(TaxRoute.SAVE_TAX_DEFINITION)
    public ApiResponse<TaxDefinitionResponseDto> saveTaxDefinition(@RequestBody @Valid TaxDefinitionRequest request) {
        return ApiResponse.SUCCESS(taxService.saveTaxDefinition(request), "Tax definition saved successfully");
    }

    @DeleteMapping(TaxRoute.DELETE_TAX_DEFINITION)
    public ApiResponse<Boolean> deleteTaxDefinition(@PathVariable Long id) {
        return ApiResponse.SUCCESS(taxService.deleteTaxDefinition(id), "Tax definition deleted successfully");
    }

    @GetMapping(TaxRoute.GET_TAX_RULES)
    public ApiResponse<PageResponse<TaxRuleResponseDto>> getTaxRulePage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size
    ) {
        return ApiResponse.SUCCESS(taxService.getTaxRulePage(chainId, restaurantId, isActive, page, size));
    }

    @PostMapping(TaxRoute.SAVE_TAX_RULE)
    public ApiResponse<TaxRuleResponseDto> saveTaxRule(@RequestBody @Valid TaxRuleRequest request) {
        return ApiResponse.SUCCESS(taxService.saveTaxRule(request), "Tax rule saved successfully");
    }

    @DeleteMapping(TaxRoute.DELETE_TAX_RULE)
    public ApiResponse<Boolean> deleteTaxRule(@PathVariable Long id) {
        return ApiResponse.SUCCESS(taxService.deleteTaxRule(id), "Tax rule deleted successfully");
    }

    @GetMapping(TaxRoute.GET_TAX_REGISTRATIONS)
    public ApiResponse<PageResponse<TaxRegistrationResponseDto>> getTaxRegistrationPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size
    ) {
        return ApiResponse.SUCCESS(taxService.getTaxRegistrationPage(chainId, restaurantId, isActive, page, size));
    }

    @PostMapping(TaxRoute.SAVE_TAX_REGISTRATION)
    public ApiResponse<TaxRegistrationResponseDto> saveTaxRegistration(@RequestBody @Valid TaxRegistrationRequest request) {
        return ApiResponse.SUCCESS(taxService.saveTaxRegistration(request), "Tax registration saved successfully");
    }

    @DeleteMapping(TaxRoute.DELETE_TAX_REGISTRATION)
    public ApiResponse<Boolean> deleteTaxRegistration(@PathVariable Long id) {
        return ApiResponse.SUCCESS(taxService.deleteTaxRegistration(id), "Tax registration deleted successfully");
    }
}
