package com.kritik.POS.tax.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.restaurant.mapper.RestaurantDtoMapper;
import com.kritik.POS.tax.TaxErrorMessage;
import com.kritik.POS.tax.dto.TaxRateResponseDto;
import com.kritik.POS.tax.dto.TaxRequest;
import com.kritik.POS.tax.entity.TaxRate;
import com.kritik.POS.tax.projection.TaxRateSummaryProjection;
import com.kritik.POS.tax.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxServiceImpl implements TaxService {

    private final TaxRateRepository taxRateRepository;
    private final TenantAccessService tenantAccessService;
    private final RestaurantDtoMapper restaurantDtoMapper;

    @Override
    public TaxRate saveTaxRate(TaxRequest taxRequest) {
        TaxRate taxRate = new TaxRate();
        if (taxRequest.taxId() != null) {
            taxRate = getTaxRateEntity(taxRequest.taxId());
        }
        taxRate.setTaxAmount(taxRequest.taxAmount());
        taxRate.setTaxName(taxRequest.taxName());
        taxRate.setActive(taxRequest.active());
        taxRate.setDeleted(false);
        taxRate.setRestaurantId(tenantAccessService.resolveAccessibleRestaurantId(taxRequest.restaurantId()));
        return taxRateRepository.save(taxRate);
    }

    @Override
    public boolean deleteTaxRate(Long taxId) {
        TaxRate taxRate = getTaxRateEntity(taxId);
        taxRate.setDeleted(true);
        taxRate.setActive(false);
        taxRateRepository.save(taxRate);
        return true;
    }

    @Override
    public List<TaxRate> getAllTaxRates() {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }
        return taxRateRepository.findTaxSummaries(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        null,
                        null,
                        PageRequest.of(0, 1000)
                ).map(projection -> {
                    TaxRate taxRate = new TaxRate();
                    taxRate.setTaxId(projection.getTaxId());
                    taxRate.setRestaurantId(projection.getRestaurantId());
                    taxRate.setTaxName(projection.getTaxName());
                    taxRate.setTaxAmount(projection.getTaxAmount());
                    taxRate.setActive(projection.getIsActive());
                    taxRate.setCreatedAt(projection.getCreatedAt());
                    taxRate.setUpdatedAt(projection.getUpdatedAt());
                    return taxRate;
                })
                .getContent();
    }

    @Override
    public List<TaxRate> getActiveTaxRates() {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }
        return taxRateRepository.findAllActiveVisible(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)
        );
    }

    @Override
    public PageResponse<TaxRateResponseDto> getTaxRatePage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }
        Page<TaxRateSummaryProjection> page = taxRateRepository.findTaxSummaries(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                isActive,
                search == null ? null : search.trim(),
                PageRequest.of(pageNumber, pageSize)
        );
        return PageResponse.from(page.map(restaurantDtoMapper::toTaxDto));
    }

    private TaxRate getTaxRateEntity(Long taxId) {
        TaxRate taxRate = taxRateRepository.findById(taxId).orElseThrow(() ->
                new AppException(TaxErrorMessage.INVALID_TAX_ID.getMessage(), HttpStatus.CONFLICT));
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(taxRate.getRestaurantId());
        }
        if (taxRate.isDeleted()) {
            throw new AppException(TaxErrorMessage.INVALID_TAX_ID.getMessage(), HttpStatus.CONFLICT);
        }
        return taxRate;
    }
}
