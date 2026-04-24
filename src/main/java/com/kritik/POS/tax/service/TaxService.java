package com.kritik.POS.tax.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.tax.dto.TaxClassRequest;
import com.kritik.POS.tax.dto.TaxClassResponseDto;
import com.kritik.POS.tax.dto.TaxCatalogSeedRequest;
import com.kritik.POS.tax.dto.TaxCatalogSeedResponse;
import com.kritik.POS.tax.dto.TaxDefinitionRequest;
import com.kritik.POS.tax.dto.TaxDefinitionResponseDto;
import com.kritik.POS.tax.dto.TaxRegistrationRequest;
import com.kritik.POS.tax.dto.TaxRegistrationResponseDto;
import com.kritik.POS.tax.dto.TaxRuleRequest;
import com.kritik.POS.tax.dto.TaxRuleResponseDto;
import com.kritik.POS.tax.entity.TaxClass;
import com.kritik.POS.tax.entity.TaxRegistration;
import com.kritik.POS.tax.model.TaxBuyerContext;
import com.kritik.POS.tax.model.TaxComputationResult;
import com.kritik.POS.tax.model.TaxableChargeComponent;
import java.util.List;

public interface TaxService {

    TaxClassResponseDto saveTaxClass(TaxClassRequest request);

    TaxDefinitionResponseDto saveTaxDefinition(TaxDefinitionRequest request);

    TaxRuleResponseDto saveTaxRule(TaxRuleRequest request);

    TaxRegistrationResponseDto saveTaxRegistration(TaxRegistrationRequest request);

    boolean deleteTaxClass(Long id);

    boolean deleteTaxDefinition(Long id);

    boolean deleteTaxRule(Long id);

    boolean deleteTaxRegistration(Long id);

    PageResponse<TaxClassResponseDto> getTaxClassPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize);

    PageResponse<TaxDefinitionResponseDto> getTaxDefinitionPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize);

    PageResponse<TaxRuleResponseDto> getTaxRulePage(Long chainId, Long restaurantId, Boolean isActive, Integer pageNumber, Integer pageSize);

    PageResponse<TaxRegistrationResponseDto> getTaxRegistrationPage(Long chainId, Long restaurantId, Boolean isActive, Integer pageNumber, Integer pageSize);

    TaxCatalogSeedResponse seedTaxCatalog(TaxCatalogSeedRequest request);

    TaxClass getOrCreateDefaultTaxClass(Long restaurantId);

    TaxClass resolveTaxClass(Long restaurantId, Long taxClassId);

    TaxRegistration getDefaultTaxRegistration(Long restaurantId);

    TaxComputationResult computeOrderTaxes(Long restaurantId, List<TaxableChargeComponent> components, TaxBuyerContext buyerContext);
}
