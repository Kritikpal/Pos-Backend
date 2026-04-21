package com.kritik.POS.tax.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.dto.TaxClassRequest;
import com.kritik.POS.tax.dto.TaxClassResponseDto;
import com.kritik.POS.tax.dto.TaxDefinitionRequest;
import com.kritik.POS.tax.dto.TaxDefinitionResponseDto;
import com.kritik.POS.tax.dto.TaxRegistrationRequest;
import com.kritik.POS.tax.dto.TaxRegistrationResponseDto;
import com.kritik.POS.tax.dto.TaxRuleRequest;
import com.kritik.POS.tax.dto.TaxRuleResponseDto;
import com.kritik.POS.tax.entity.TaxClass;
import com.kritik.POS.tax.entity.TaxDefinition;
import com.kritik.POS.tax.entity.TaxRegistration;
import com.kritik.POS.tax.entity.TaxRule;
import com.kritik.POS.tax.entity.enums.TaxCalculationMode;
import com.kritik.POS.tax.entity.enums.TaxCompoundMode;
import com.kritik.POS.tax.entity.enums.TaxDefinitionKind;
import com.kritik.POS.tax.entity.enums.TaxValueType;
import com.kritik.POS.tax.model.AppliedTaxComponent;
import com.kritik.POS.tax.model.TaxBuyerContext;
import com.kritik.POS.tax.model.TaxComputationResult;
import com.kritik.POS.tax.model.TaxableChargeComponent;
import com.kritik.POS.tax.repository.TaxClassRepository;
import com.kritik.POS.tax.repository.TaxDefinitionRepository;
import com.kritik.POS.tax.repository.TaxRegistrationRepository;
import com.kritik.POS.tax.repository.TaxRuleRepository;
import com.kritik.POS.tax.util.MoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxServiceImpl implements TaxService {

    private static final String DEFAULT_TAX_CLASS_CODE = "STANDARD";
    private static final String DEFAULT_TAX_CLASS_NAME = "Standard Taxable";

    private final TaxClassRepository taxClassRepository;
    private final TaxDefinitionRepository taxDefinitionRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final TaxRegistrationRepository taxRegistrationRepository;
    private final RestaurantRepository restaurantRepository;
    private final TenantAccessService tenantAccessService;

    @Override
    public TaxClassResponseDto saveTaxClass(TaxClassRequest request) {
        Long restaurantId = tenantAccessService.resolveManageableRestaurantId(request.restaurantId());
        TaxClass entity = request.id() == null ? new TaxClass() : getTaxClassEntity(request.id());
        entity.setRestaurantId(restaurantId);
        entity.setCode(normalizeCode(request.code()));
        entity.setName(request.name().trim());
        entity.setDescription(trimToNull(request.description()));
        entity.setExempt(Boolean.TRUE.equals(request.isExempt()));
        entity.setActive(request.isActive() == null || request.isActive());
        entity.setDeleted(false);
        return toDto(taxClassRepository.save(entity));
    }

    @Override
    public TaxDefinitionResponseDto saveTaxDefinition(TaxDefinitionRequest request) {
        Long restaurantId = tenantAccessService.resolveManageableRestaurantId(request.restaurantId());
        TaxDefinition entity = request.id() == null ? new TaxDefinition() : getTaxDefinitionEntity(request.id());
        entity.setRestaurantId(restaurantId);
        entity.setCode(normalizeCode(request.code()));
        entity.setDisplayName(request.displayName().trim());
        entity.setKind(request.kind());
        entity.setValueType(request.valueType());
        entity.setDefaultValue(MoneyUtils.rate(request.defaultValue()));
        entity.setCurrencyCode(defaultCurrency(request.currencyCode()));
        entity.setRecoverable(request.isRecoverable() == null || request.isRecoverable());
        entity.setActive(request.isActive() == null || request.isActive());
        entity.setDeleted(false);
        return toDto(taxDefinitionRepository.save(entity));
    }

    @Override
    public TaxRuleResponseDto saveTaxRule(TaxRuleRequest request) {
        Long restaurantId = tenantAccessService.resolveManageableRestaurantId(request.restaurantId());
        TaxDefinition taxDefinition = getTaxDefinitionEntity(request.taxDefinitionId());
        TaxClass taxClass = getTaxClassEntity(request.taxClassId());
        if (!Objects.equals(taxDefinition.getRestaurantId(), restaurantId) || !Objects.equals(taxClass.getRestaurantId(), restaurantId)) {
            throw new AppException("Tax rule entities must belong to the same restaurant", HttpStatus.BAD_REQUEST);
        }

        TaxRule entity = request.id() == null ? new TaxRule() : getTaxRuleEntity(request.id());
        entity.setRestaurantId(restaurantId);
        entity.setTaxDefinitionId(taxDefinition.getId());
        entity.setTaxClassId(taxClass.getId());
        entity.setCalculationMode(request.calculationMode());
        entity.setCompoundMode(request.compoundMode());
        entity.setSequenceNo(request.sequenceNo());
        entity.setValidFrom(request.validFrom());
        entity.setValidTo(request.validTo());
        entity.setCountryCode(normalizeCodeNullable(request.countryCode()));
        entity.setRegionCode(normalizeCodeNullable(request.regionCode()));
        entity.setBuyerTaxCategory(trimToNull(request.buyerTaxCategory()));
        entity.setMinAmount(request.minAmount() == null ? null : MoneyUtils.money(request.minAmount()));
        entity.setMaxAmount(request.maxAmount() == null ? null : MoneyUtils.money(request.maxAmount()));
        entity.setPriority(request.priority() == null ? 0 : request.priority());
        entity.setActive(request.isActive() == null || request.isActive());
        entity.setDeleted(false);
        return toDto(taxRuleRepository.save(entity));
    }

    @Override
    public TaxRegistrationResponseDto saveTaxRegistration(TaxRegistrationRequest request) {
        Long restaurantId = tenantAccessService.resolveManageableRestaurantId(request.restaurantId());
        TaxRegistration entity = request.id() == null ? new TaxRegistration() : getTaxRegistrationEntity(request.id());
        entity.setRestaurantId(restaurantId);
        entity.setSchemeCode(normalizeCode(request.schemeCode()));
        entity.setRegistrationNumber(request.registrationNumber().trim());
        entity.setLegalName(request.legalName().trim());
        entity.setCountryCode(normalizeCodeNullable(request.countryCode()));
        entity.setRegionCode(normalizeCodeNullable(request.regionCode()));
        entity.setPlaceOfBusiness(trimToNull(request.placeOfBusiness()));
        entity.setDefault(Boolean.TRUE.equals(request.isDefault()));
        entity.setValidFrom(request.validFrom());
        entity.setValidTo(request.validTo());
        entity.setActive(request.isActive() == null || request.isActive());
        if (entity.isDefault()) {
            clearExistingDefaultRegistration(restaurantId, entity.getId());
        }
        return toDto(taxRegistrationRepository.save(entity));
    }

    @Override
    public boolean deleteTaxClass(Long id) {
        TaxClass entity = getTaxClassEntity(id);
        entity.setDeleted(true);
        entity.setActive(false);
        taxClassRepository.save(entity);
        return true;
    }

    @Override
    public boolean deleteTaxDefinition(Long id) {
        TaxDefinition entity = getTaxDefinitionEntity(id);
        entity.setDeleted(true);
        entity.setActive(false);
        taxDefinitionRepository.save(entity);
        return true;
    }

    @Override
    public boolean deleteTaxRule(Long id) {
        TaxRule entity = getTaxRuleEntity(id);
        entity.setDeleted(true);
        entity.setActive(false);
        taxRuleRepository.save(entity);
        return true;
    }

    @Override
    public boolean deleteTaxRegistration(Long id) {
        TaxRegistration entity = getTaxRegistrationEntity(id);
        entity.setActive(false);
        taxRegistrationRepository.save(entity);
        return true;
    }

    @Override
    public PageResponse<TaxClassResponseDto> getTaxClassPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize) {
        Page<TaxClass> page = taxClassRepository.findVisible(
                tenantAccessService.isSuperAdmin(),
                resolveVisibleRestaurantIds(chainId, restaurantId),
                isActive,
                normalizeSearch(search),
                PageRequest.of(pageNumber, pageSize)
        );
        return PageResponse.from(page.map(this::toDto));
    }

    @Override
    public PageResponse<TaxDefinitionResponseDto> getTaxDefinitionPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize) {
        Page<TaxDefinition> page = taxDefinitionRepository.findVisible(
                tenantAccessService.isSuperAdmin(),
                resolveVisibleRestaurantIds(chainId, restaurantId),
                isActive,
                normalizeSearch(search),
                PageRequest.of(pageNumber, pageSize)
        );
        return PageResponse.from(page.map(this::toDto));
    }

    @Override
    public PageResponse<TaxRuleResponseDto> getTaxRulePage(Long chainId, Long restaurantId, Boolean isActive, Integer pageNumber, Integer pageSize) {
        Page<TaxRule> page = taxRuleRepository.findVisible(
                tenantAccessService.isSuperAdmin(),
                resolveVisibleRestaurantIds(chainId, restaurantId),
                isActive,
                PageRequest.of(pageNumber, pageSize)
        );
        return PageResponse.from(page.map(this::toDto));
    }

    @Override
    public PageResponse<TaxRegistrationResponseDto> getTaxRegistrationPage(Long chainId, Long restaurantId, Boolean isActive, Integer pageNumber, Integer pageSize) {
        Page<TaxRegistration> page = taxRegistrationRepository.findVisible(
                tenantAccessService.isSuperAdmin(),
                resolveVisibleRestaurantIds(chainId, restaurantId),
                isActive,
                PageRequest.of(pageNumber, pageSize)
        );
        return PageResponse.from(page.map(this::toDto));
    }

    @Override
    public TaxClass getOrCreateDefaultTaxClass(Long restaurantId) {
        Long accessibleRestaurantId = tenantAccessService.resolveAccessibleRestaurantId(restaurantId);
        return taxClassRepository.findByRestaurantIdAndCodeAndIsDeletedFalse(accessibleRestaurantId, DEFAULT_TAX_CLASS_CODE)
                .orElseGet(() -> {
                    TaxClass taxClass = new TaxClass();
                    taxClass.setRestaurantId(accessibleRestaurantId);
                    taxClass.setCode(DEFAULT_TAX_CLASS_CODE);
                    taxClass.setName(DEFAULT_TAX_CLASS_NAME);
                    taxClass.setExempt(false);
                    taxClass.setActive(true);
                    taxClass.setDeleted(false);
                    return taxClassRepository.save(taxClass);
                });
    }

    @Override
    public TaxClass resolveTaxClass(Long restaurantId, Long taxClassId) {
        if (taxClassId == null) {
            return getOrCreateDefaultTaxClass(restaurantId);
        }
        TaxClass taxClass = getTaxClassEntity(taxClassId);
        if (!Objects.equals(taxClass.getRestaurantId(), tenantAccessService.resolveAccessibleRestaurantId(restaurantId))) {
            throw new AppException("Tax class does not belong to the requested restaurant", HttpStatus.BAD_REQUEST);
        }
        return taxClass;
    }

    @Override
    public TaxRegistration getDefaultTaxRegistration(Long restaurantId) {
        Long accessibleRestaurantId = tenantAccessService.resolveAccessibleRestaurantId(restaurantId);
        return taxRegistrationRepository.findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(accessibleRestaurantId)
                .orElseGet(() -> bootstrapDefaultRegistration(accessibleRestaurantId));
    }

    @Override
    public TaxComputationResult computeOrderTaxes(Long restaurantId,
                                                  List<TaxableChargeComponent> components,
                                                  TaxBuyerContext buyerContext) {
        if (components == null || components.isEmpty()) {
            return emptyComputation();
        }

        Long accessibleRestaurantId = tenantAccessService.resolveAccessibleRestaurantId(restaurantId);
        List<TaxRule> activeRules = taxRuleRepository.findActiveForRestaurant(accessibleRestaurantId);
        Map<Long, TaxDefinition> definitionsById = taxDefinitionRepository.findAllByIdIn(
                        activeRules.stream().map(TaxRule::getTaxDefinitionId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(TaxDefinition::getId, value -> value));
        Map<Long, TaxClass> taxClassById = taxClassRepository.findAllById(
                        components.stream()
                                .map(TaxableChargeComponent::taxClassId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(TaxClass::getId, value -> value));

        BigDecimal subtotal = MoneyUtils.zero();
        BigDecimal taxableAmount = MoneyUtils.zero();
        BigDecimal taxAmount = MoneyUtils.zero();
        BigDecimal feeAmount = MoneyUtils.zero();
        BigDecimal grandTotal = MoneyUtils.zero();
        List<AppliedTaxComponent> appliedTaxes = new ArrayList<>();

        for (TaxableChargeComponent component : components) {
            BigDecimal componentAmount = MoneyUtils.money(component.taxableAmount());
            subtotal = MoneyUtils.add(subtotal, componentAmount);

            TaxClass taxClass = component.taxClassId() == null ? null : taxClassById.get(component.taxClassId());
            if (taxClass == null || taxClass.isDeleted() || taxClass.isExempt()) {
                taxableAmount = MoneyUtils.add(taxableAmount, componentAmount);
                grandTotal = MoneyUtils.add(grandTotal, componentAmount);
                continue;
            }

            List<RuleWithDefinition> matches = activeRules.stream()
                    .filter(rule -> isRuleMatch(rule, taxClass, componentAmount, buyerContext))
                    .map(rule -> new RuleWithDefinition(rule, definitionsById.get(rule.getTaxDefinitionId())))
                    .filter(candidate -> candidate.definition() != null && candidate.definition().isActive() && !candidate.definition().isDeleted())
                    .sorted(Comparator
                            .comparing((RuleWithDefinition candidate) -> candidate.rule().getSequenceNo())
                            .thenComparing((RuleWithDefinition candidate) -> candidate.rule().getPriority(), Comparator.reverseOrder())
                            .thenComparing(candidate -> candidate.rule().getId()))
                    .toList();

            ComponentTaxResult componentTaxResult = computeComponentTaxes(component, componentAmount, matches);
            taxableAmount = MoneyUtils.add(taxableAmount, componentTaxResult.taxableAmount());
            taxAmount = MoneyUtils.add(taxAmount, componentTaxResult.taxAmount());
            feeAmount = MoneyUtils.add(feeAmount, componentTaxResult.feeAmount());
            grandTotal = MoneyUtils.add(grandTotal, componentTaxResult.grandTotal());
            appliedTaxes.addAll(componentTaxResult.appliedTaxes());
        }

        return new TaxComputationResult(
                subtotal,
                MoneyUtils.zero(),
                taxableAmount,
                taxAmount,
                feeAmount,
                grandTotal,
                appliedTaxes
        );
    }

    private ComponentTaxResult computeComponentTaxes(TaxableChargeComponent component,
                                                     BigDecimal componentAmount,
                                                     List<RuleWithDefinition> matches) {
        BigDecimal inclusiveTotal = MoneyUtils.zero();
        BigDecimal runningInclusiveBase = componentAmount;
        List<AppliedTaxComponent> componentApplied = new ArrayList<>();

        for (RuleWithDefinition match : matches.stream()
                .filter(candidate -> candidate.rule().getCalculationMode() == TaxCalculationMode.INCLUSIVE)
                .toList()) {
            BigDecimal ruleBase = runningInclusiveBase;
            BigDecimal computedTax = computeTax(ruleBase, match.definition().getDefaultValue(), match.definition().getValueType(), true);
            runningInclusiveBase = MoneyUtils.subtract(runningInclusiveBase, computedTax);
            inclusiveTotal = MoneyUtils.add(inclusiveTotal, computedTax);
            componentApplied.add(toAppliedTax(component.referenceKey(), match, ruleBase, computedTax));
        }

        BigDecimal taxableBase = MoneyUtils.subtract(componentAmount, inclusiveTotal);
        BigDecimal exclusiveTotal = MoneyUtils.zero();
        BigDecimal previousExclusive = MoneyUtils.zero();

        for (RuleWithDefinition match : matches.stream()
                .filter(candidate -> candidate.rule().getCalculationMode() == TaxCalculationMode.EXCLUSIVE)
                .toList()) {
            BigDecimal ruleBase = match.rule().getCompoundMode() == TaxCompoundMode.ON_PREVIOUS_TAXES
                    ? MoneyUtils.add(taxableBase, previousExclusive)
                    : taxableBase;
            BigDecimal computedTax = computeTax(ruleBase, match.definition().getDefaultValue(), match.definition().getValueType(), false);
            previousExclusive = MoneyUtils.add(previousExclusive, computedTax);
            exclusiveTotal = MoneyUtils.add(exclusiveTotal, computedTax);
            componentApplied.add(toAppliedTax(component.referenceKey(), match, ruleBase, computedTax));
        }

        BigDecimal componentTax = MoneyUtils.zero();
        BigDecimal componentFee = MoneyUtils.zero();
        for (AppliedTaxComponent applied : componentApplied) {
            if (applied.kind() == TaxDefinitionKind.TAX) {
                componentTax = MoneyUtils.add(componentTax, applied.taxAmount());
            } else {
                componentFee = MoneyUtils.add(componentFee, applied.taxAmount());
            }
        }

        return new ComponentTaxResult(
                taxableBase,
                componentTax,
                componentFee,
                MoneyUtils.add(componentAmount, exclusiveTotal),
                componentApplied
        );
    }

    private TaxRegistration bootstrapDefaultRegistration(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId).orElse(null);
        if (restaurant == null || restaurant.getGstNumber() == null || restaurant.getGstNumber().isBlank()) {
            return null;
        }
        TaxRegistration registration = new TaxRegistration();
        registration.setRestaurantId(restaurantId);
        registration.setSchemeCode("GST");
        registration.setRegistrationNumber(restaurant.getGstNumber().trim());
        registration.setLegalName(restaurant.getName());
        registration.setCountryCode(normalizeCodeNullable(restaurant.getCountry()));
        registration.setRegionCode(normalizeCodeNullable(restaurant.getState()));
        registration.setPlaceOfBusiness(trimToNull(restaurant.getAddressLine1()));
        registration.setDefault(true);
        registration.setActive(true);
        return taxRegistrationRepository.save(registration);
    }

    private void clearExistingDefaultRegistration(Long restaurantId, Long currentId) {
        Page<TaxRegistration> existing = taxRegistrationRepository.findVisible(false, List.of(restaurantId), null, PageRequest.of(0, 100));
        for (TaxRegistration registration : existing) {
            if (registration.isDefault() && !Objects.equals(registration.getId(), currentId)) {
                registration.setDefault(false);
                taxRegistrationRepository.save(registration);
            }
        }
    }

    private boolean isRuleMatch(TaxRule rule,
                                TaxClass taxClass,
                                BigDecimal componentAmount,
                                TaxBuyerContext buyerContext) {
        if (rule.isDeleted() || !rule.isActive() || !Objects.equals(rule.getTaxClassId(), taxClass.getId())) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (rule.getValidFrom() != null && today.isBefore(rule.getValidFrom())) {
            return false;
        }
        if (rule.getValidTo() != null && today.isAfter(rule.getValidTo())) {
            return false;
        }
        if (rule.getCountryCode() != null) {
            String supplyCountry = firstNonBlank(
                    buyerContext == null ? null : buyerContext.placeOfSupplyCountryCode(),
                    buyerContext == null ? null : buyerContext.buyerCountryCode()
            );
            if (!normalizeCode(rule.getCountryCode()).equals(normalizeCodeNullable(supplyCountry))) {
                return false;
            }
        }
        if (rule.getRegionCode() != null) {
            String supplyRegion = firstNonBlank(
                    buyerContext == null ? null : buyerContext.placeOfSupplyRegionCode(),
                    buyerContext == null ? null : buyerContext.buyerRegionCode()
            );
            if (!normalizeCode(rule.getRegionCode()).equals(normalizeCodeNullable(supplyRegion))) {
                return false;
            }
        }
        if (rule.getBuyerTaxCategory() != null) {
            String buyerCategory = buyerContext == null ? null : buyerContext.buyerTaxCategory();
            if (!rule.getBuyerTaxCategory().equalsIgnoreCase(firstNonBlank(buyerCategory, ""))) {
                return false;
            }
        }
        if (rule.getMinAmount() != null && componentAmount.compareTo(rule.getMinAmount()) < 0) {
            return false;
        }
        if (rule.getMaxAmount() != null && componentAmount.compareTo(rule.getMaxAmount()) > 0) {
            return false;
        }
        return true;
    }

    private BigDecimal computeTax(BigDecimal base, BigDecimal configuredValue, TaxValueType valueType, boolean inclusive) {
        BigDecimal value = valueType == TaxValueType.PERCENT ? MoneyUtils.rate(configuredValue) : MoneyUtils.money(configuredValue);
        if (valueType == TaxValueType.FIXED) {
            return MoneyUtils.money(value);
        }
        return inclusive ? MoneyUtils.inclusivePercentTax(base, value) : MoneyUtils.percentOf(base, value);
    }

    private AppliedTaxComponent toAppliedTax(String referenceKey,
                                             RuleWithDefinition match,
                                             BigDecimal taxableBaseAmount,
                                             BigDecimal taxAmount) {
        TaxRule rule = match.rule();
        TaxDefinition definition = match.definition();
        return new AppliedTaxComponent(
                referenceKey,
                definition.getCode(),
                definition.getDisplayName(),
                definition.getKind(),
                definition.getValueType(),
                definition.getDefaultValue(),
                rule.getCalculationMode(),
                rule.getCompoundMode(),
                rule.getSequenceNo(),
                MoneyUtils.money(taxableBaseAmount),
                MoneyUtils.money(taxAmount),
                definition.getCurrencyCode(),
                rule.getCountryCode(),
                rule.getRegionCode()
        );
    }

    private TaxClass getTaxClassEntity(Long id) {
        TaxClass entity = taxClassRepository.findById(id)
                .orElseThrow(() -> new AppException("Invalid tax class id", HttpStatus.BAD_REQUEST));
        tenantAccessService.resolveAccessibleRestaurantId(entity.getRestaurantId());
        if (entity.isDeleted()) {
            throw new AppException("Invalid tax class id", HttpStatus.BAD_REQUEST);
        }
        return entity;
    }

    private TaxDefinition getTaxDefinitionEntity(Long id) {
        TaxDefinition entity = taxDefinitionRepository.findById(id)
                .orElseThrow(() -> new AppException("Invalid tax definition id", HttpStatus.BAD_REQUEST));
        tenantAccessService.resolveAccessibleRestaurantId(entity.getRestaurantId());
        if (entity.isDeleted()) {
            throw new AppException("Invalid tax definition id", HttpStatus.BAD_REQUEST);
        }
        return entity;
    }

    private TaxRule getTaxRuleEntity(Long id) {
        TaxRule entity = taxRuleRepository.findById(id)
                .orElseThrow(() -> new AppException("Invalid tax rule id", HttpStatus.BAD_REQUEST));
        tenantAccessService.resolveAccessibleRestaurantId(entity.getRestaurantId());
        if (entity.isDeleted()) {
            throw new AppException("Invalid tax rule id", HttpStatus.BAD_REQUEST);
        }
        return entity;
    }

    private TaxRegistration getTaxRegistrationEntity(Long id) {
        TaxRegistration entity = taxRegistrationRepository.findById(id)
                .orElseThrow(() -> new AppException("Invalid tax registration id", HttpStatus.BAD_REQUEST));
        tenantAccessService.resolveAccessibleRestaurantId(entity.getRestaurantId());
        return entity;
    }

    private TaxClassResponseDto toDto(TaxClass entity) {
        return new TaxClassResponseDto(
                entity.getId(),
                entity.getRestaurantId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isExempt(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private TaxDefinitionResponseDto toDto(TaxDefinition entity) {
        return new TaxDefinitionResponseDto(
                entity.getId(),
                entity.getRestaurantId(),
                entity.getCode(),
                entity.getDisplayName(),
                entity.getKind(),
                entity.getValueType(),
                entity.getDefaultValue(),
                entity.getCurrencyCode(),
                entity.isRecoverable(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private TaxRuleResponseDto toDto(TaxRule entity) {
        return new TaxRuleResponseDto(
                entity.getId(),
                entity.getRestaurantId(),
                entity.getTaxDefinitionId(),
                entity.getTaxClassId(),
                entity.getCalculationMode(),
                entity.getCompoundMode(),
                entity.getSequenceNo(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getCountryCode(),
                entity.getRegionCode(),
                entity.getBuyerTaxCategory(),
                entity.getMinAmount(),
                entity.getMaxAmount(),
                entity.getPriority(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private TaxRegistrationResponseDto toDto(TaxRegistration entity) {
        return new TaxRegistrationResponseDto(
                entity.getId(),
                entity.getRestaurantId(),
                entity.getSchemeCode(),
                entity.getRegistrationNumber(),
                entity.getLegalName(),
                entity.getCountryCode(),
                entity.getRegionCode(),
                entity.getPlaceOfBusiness(),
                entity.isDefault(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private TaxComputationResult emptyComputation() {
        return new TaxComputationResult(
                MoneyUtils.zero(),
                MoneyUtils.zero(),
                MoneyUtils.zero(),
                MoneyUtils.zero(),
                MoneyUtils.zero(),
                MoneyUtils.zero(),
                List.of()
        );
    }

    private List<Long> resolveVisibleRestaurantIds(Long chainId, Long restaurantId) {
        return tenantAccessService.queryRestaurantIds(
                tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId)
        );
    }

    private String normalizeSearch(String search) {
        return trimToNull(search);
    }

    private String defaultCurrency(String currencyCode) {
        String normalized = trimToNull(currencyCode);
        return normalized == null ? "INR" : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCodeNullable(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String first, String second) {
        String resolvedFirst = trimToNull(first);
        return resolvedFirst != null ? resolvedFirst : trimToNull(second);
    }

    private record RuleWithDefinition(TaxRule rule, TaxDefinition definition) {
    }

    private record ComponentTaxResult(BigDecimal taxableAmount,
                                      BigDecimal taxAmount,
                                      BigDecimal feeAmount,
                                      BigDecimal grandTotal,
                                      List<AppliedTaxComponent> appliedTaxes) {
    }
}
