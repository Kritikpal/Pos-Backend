package com.kritik.POS.tax.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.dto.TaxClassRequest;
import com.kritik.POS.tax.dto.TaxClassResponseDto;
import com.kritik.POS.tax.dto.TaxCatalogSeedCountryResult;
import com.kritik.POS.tax.dto.TaxCatalogSeedRequest;
import com.kritik.POS.tax.dto.TaxCatalogSeedResponse;
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
import com.kritik.POS.tax.entity.enums.TaxSupplyScope;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxServiceImpl implements TaxService {

    private static final String DEFAULT_TAX_CLASS_CODE = "STANDARD";
    private static final String DEFAULT_TAX_CLASS_NAME = "Standard Taxable";
    private static final String INDIA_COUNTRY_CODE = "IN";
    private static final String INDIA_DATASET_PREFIX = "IN_GST_FY";

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
        entity.setSupplyScope(TaxSupplyScope.ANY);
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
    @Transactional
    public TaxCatalogSeedResponse seedTaxCatalog(TaxCatalogSeedRequest request) {
        Long restaurantId = tenantAccessService.resolveManageableRestaurantId(request.restaurantId());
        LocalDate effectiveFrom = request.effectiveFrom() == null ? currentIndianFinancialYearStart() : request.effectiveFrom();
        boolean overwriteExisting = Boolean.TRUE.equals(request.overwriteExisting());

        List<TaxCatalogSeedCountryResult> results = new ArrayList<>();
        for (String requestedCountry : request.countries()) {
            String countryCode = normalizeCountryInput(requestedCountry);
            if (!INDIA_COUNTRY_CODE.equals(countryCode)) {
                throw new AppException("Only India tax catalog seeding is supported right now", HttpStatus.BAD_REQUEST);
            }
            results.add(seedIndiaTaxCatalog(restaurantId, effectiveFrom, overwriteExisting));
        }

        return new TaxCatalogSeedResponse(restaurantId, results);
    }

    private TaxCatalogSeedCountryResult seedIndiaTaxCatalog(Long restaurantId,
                                                            LocalDate effectiveFrom,
                                                            boolean overwriteExisting) {
        Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)
                .orElseThrow(() -> new AppException("Invalid restaurant id", HttpStatus.BAD_REQUEST));
        TaxRegistration existingRegistration = taxRegistrationRepository
                .findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(restaurantId)
                .orElse(null);
        String sellerRegionCode = resolveSellerRegionCode(restaurant, existingRegistration);
        String datasetCode = buildIndiaDatasetCode(effectiveFrom);
        List<String> warnings = new ArrayList<>();
        SeedCounter counter = new SeedCounter();

        List<TaxClass> existingClasses = taxClassRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId);
        Map<String, TaxClass> classByCode = existingClasses.stream()
                .collect(Collectors.toMap(entity -> normalizeCode(entity.getCode()), entity -> entity, (left, right) -> left, LinkedHashMap::new));

        for (TaxClassSeed seed : buildIndiaTaxClassSeeds()) {
            upsertTaxClassSeed(restaurantId, classByCode, seed, overwriteExisting, counter);
        }

        List<TaxDefinition> existingDefinitions = taxDefinitionRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId);
        Map<String, TaxDefinition> definitionByCode = existingDefinitions.stream()
                .collect(Collectors.toMap(entity -> normalizeCode(entity.getCode()), entity -> entity, (left, right) -> left, LinkedHashMap::new));

        for (TaxDefinitionSeed seed : buildIndiaTaxDefinitionSeeds()) {
            upsertTaxDefinitionSeed(restaurantId, definitionByCode, seed, overwriteExisting, counter);
        }

        List<TaxRule> existingRules = taxRuleRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId);
        Map<String, TaxRule> ruleByKey = new LinkedHashMap<>();
        for (TaxRule rule : existingRules) {
            ruleByKey.put(ruleSeedKey(rule), rule);
        }

        for (TaxRuleSeed seed : buildIndiaTaxRuleSeeds(sellerRegionCode, effectiveFrom)) {
            TaxClass taxClass = classByCode.get(seed.taxClassCode());
            TaxDefinition definition = definitionByCode.get(seed.taxDefinitionCode());
            if (taxClass == null || definition == null) {
                continue;
            }
            upsertTaxRuleSeed(restaurantId, taxClass, definition, ruleByKey, seed, overwriteExisting, counter);
        }

        boolean hadDefaultRegistration = taxRegistrationRepository
                .findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(restaurantId)
                .isPresent();
        TaxRegistration registration = ensureIndiaDefaultRegistration(restaurant, sellerRegionCode, warnings);
        if (registration == null) {
            warnings.add("Default GST registration was not created because the restaurant GST number is missing.");
        } else {
            counter.defaultRegistrationCreated = !hadDefaultRegistration;
        }
        if (sellerRegionCode == null) {
            warnings.add("State/region could not be derived from restaurant state or GSTIN. Interstate GST rule matching will need a valid seller region.");
        }
        warnings.add("Compensation cess and state-specific non-GST taxes are not seeded by this API.");

        return new TaxCatalogSeedCountryResult(
                INDIA_COUNTRY_CODE,
                datasetCode,
                effectiveFrom,
                counter.taxClassesCreated,
                counter.taxClassesUpdated,
                counter.taxDefinitionsCreated,
                counter.taxDefinitionsUpdated,
                counter.taxRulesCreated,
                counter.taxRulesUpdated,
                counter.defaultRegistrationCreated,
                registration == null ? null : registration.getRegistrationNumber(),
                warnings
        );
    }

    private void upsertTaxClassSeed(Long restaurantId,
                                    Map<String, TaxClass> classByCode,
                                    TaxClassSeed seed,
                                    boolean overwriteExisting,
                                    SeedCounter counter) {
        TaxClass entity = classByCode.get(seed.code());
        if (entity == null) {
            entity = new TaxClass();
            entity.setRestaurantId(restaurantId);
            entity.setCode(seed.code());
            counter.taxClassesCreated++;
        } else {
            counter.taxClassesUpdated++;
        }
        if (entity.getDescription() == null || overwriteExisting) {
            entity.setDescription(seed.description());
        }
        if (entity.getName() == null || overwriteExisting || DEFAULT_TAX_CLASS_CODE.equals(seed.code())) {
            entity.setName(seed.name());
        }
        entity.setExempt(seed.exempt());
        entity.setActive(true);
        entity.setDeleted(false);
        TaxClass saved = taxClassRepository.save(entity);
        classByCode.put(normalizeCode(saved.getCode()), saved);
    }

    private void upsertTaxDefinitionSeed(Long restaurantId,
                                         Map<String, TaxDefinition> definitionByCode,
                                         TaxDefinitionSeed seed,
                                         boolean overwriteExisting,
                                         SeedCounter counter) {
        TaxDefinition entity = definitionByCode.get(seed.code());
        if (entity == null) {
            entity = new TaxDefinition();
            entity.setRestaurantId(restaurantId);
            entity.setCode(seed.code());
            counter.taxDefinitionsCreated++;
        } else {
            counter.taxDefinitionsUpdated++;
        }
        entity.setDisplayName(seed.displayName());
        entity.setKind(seed.kind());
        entity.setValueType(seed.valueType());
        if (overwriteExisting || entity.getDefaultValue() == null || BigDecimal.ZERO.compareTo(entity.getDefaultValue()) == 0) {
            entity.setDefaultValue(MoneyUtils.rate(seed.defaultValue()));
        }
        entity.setCurrencyCode("INR");
        entity.setRecoverable(seed.recoverable());
        entity.setActive(true);
        entity.setDeleted(false);
        TaxDefinition saved = taxDefinitionRepository.save(entity);
        definitionByCode.put(normalizeCode(saved.getCode()), saved);
    }

    private void upsertTaxRuleSeed(Long restaurantId,
                                   TaxClass taxClass,
                                   TaxDefinition definition,
                                   Map<String, TaxRule> ruleByKey,
                                   TaxRuleSeed seed,
                                   boolean overwriteExisting,
                                   SeedCounter counter) {
        String key = ruleSeedKey(
                restaurantId,
                definition.getId(),
                taxClass.getId(),
                seed.countryCode(),
                seed.regionCode(),
                seed.supplyScope(),
                seed.sequenceNo()
        );
        TaxRule entity = ruleByKey.get(key);
        if (entity == null) {
            entity = new TaxRule();
            entity.setRestaurantId(restaurantId);
            entity.setTaxDefinitionId(definition.getId());
            entity.setTaxClassId(taxClass.getId());
            counter.taxRulesCreated++;
        } else {
            counter.taxRulesUpdated++;
        }
        entity.setCalculationMode(seed.calculationMode());
        entity.setCompoundMode(seed.compoundMode());
        entity.setSequenceNo(seed.sequenceNo());
        if (overwriteExisting || entity.getValidFrom() == null) {
            entity.setValidFrom(seed.validFrom());
        }
        entity.setValidTo(null);
        entity.setCountryCode(seed.countryCode());
        entity.setRegionCode(seed.regionCode());
        entity.setSupplyScope(seed.supplyScope());
        entity.setBuyerTaxCategory(null);
        entity.setMinAmount(null);
        entity.setMaxAmount(null);
        entity.setPriority(seed.priority());
        entity.setActive(true);
        entity.setDeleted(false);
        TaxRule saved = taxRuleRepository.save(entity);
        ruleByKey.put(key, saved);
    }

    private TaxRegistration ensureIndiaDefaultRegistration(Restaurant restaurant,
                                                           String sellerRegionCode,
                                                           List<String> warnings) {
        TaxRegistration existing = taxRegistrationRepository
                .findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(restaurant.getRestaurantId())
                .orElse(null);
        if (existing != null) {
            if (existing.getCountryCode() == null) {
                existing.setCountryCode(INDIA_COUNTRY_CODE);
            }
            if (existing.getRegionCode() == null && sellerRegionCode != null) {
                existing.setRegionCode(sellerRegionCode);
            }
            existing.setActive(true);
            return taxRegistrationRepository.save(existing);
        }
        if (restaurant.getGstNumber() == null || restaurant.getGstNumber().isBlank()) {
            return null;
        }

        TaxRegistration registration = new TaxRegistration();
        registration.setRestaurantId(restaurant.getRestaurantId());
        registration.setSchemeCode("GST");
        registration.setRegistrationNumber(restaurant.getGstNumber().trim());
        registration.setLegalName(restaurant.getName());
        registration.setCountryCode(INDIA_COUNTRY_CODE);
        registration.setRegionCode(sellerRegionCode);
        registration.setPlaceOfBusiness(buildPlaceOfBusiness(restaurant));
        registration.setDefault(true);
        registration.setActive(true);
        if (sellerRegionCode == null) {
            warnings.add("Default GST registration was created, but its state code could not be normalized.");
        }
        return taxRegistrationRepository.save(registration);
    }

    private List<TaxClassSeed> buildIndiaTaxClassSeeds() {
        return List.of(
                new TaxClassSeed(DEFAULT_TAX_CLASS_CODE, "Standard Restaurant GST", "Default 5% GST class for restaurant service sold through the POS.", false),
                new TaxClassSeed("GST_EXEMPT", "GST Exempt", "Exempt or nil-rated supplies under GST.", true),
                new TaxClassSeed("GST_OUT_OF_SCOPE", "Out Of GST Scope", "Supplies outside GST, such as alcoholic liquor for human consumption.", true),
                new TaxClassSeed("GST_5", "GST 5%", "General 5% GST class for taxable goods or services.", false),
                new TaxClassSeed("GST_12", "GST 12%", "General 12% GST class for taxable goods or services.", false),
                new TaxClassSeed("GST_18", "GST 18%", "General 18% GST class for taxable goods or services.", false),
                new TaxClassSeed("GST_28", "GST 28%", "General 28% GST class for taxable goods or services.", false),
                new TaxClassSeed("GST_RESTAURANT_PREMISES_18", "Restaurant 18% (Specified Premises)", "Restaurant service at specified premises taxable at 18%.", false)
        );
    }

    private List<TaxDefinitionSeed> buildIndiaTaxDefinitionSeeds() {
        return List.of(
                new TaxDefinitionSeed("IN_RESTAURANT_CGST_2_5", "Restaurant CGST 2.5%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(2.5), false),
                new TaxDefinitionSeed("IN_RESTAURANT_SGST_2_5", "Restaurant SGST 2.5%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(2.5), false),
                new TaxDefinitionSeed("IN_RESTAURANT_IGST_5", "Restaurant IGST 5%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(5), false),
                new TaxDefinitionSeed("IN_GST5_CGST_2_5", "GST 5% CGST 2.5%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(2.5), true),
                new TaxDefinitionSeed("IN_GST5_SGST_2_5", "GST 5% SGST 2.5%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(2.5), true),
                new TaxDefinitionSeed("IN_GST5_IGST_5", "GST 5% IGST 5%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(5), true),
                new TaxDefinitionSeed("IN_GST12_CGST_6", "GST 12% CGST 6%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(6), true),
                new TaxDefinitionSeed("IN_GST12_SGST_6", "GST 12% SGST 6%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(6), true),
                new TaxDefinitionSeed("IN_GST12_IGST_12", "GST 12% IGST 12%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(12), true),
                new TaxDefinitionSeed("IN_GST18_CGST_9", "GST 18% CGST 9%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(9), true),
                new TaxDefinitionSeed("IN_GST18_SGST_9", "GST 18% SGST 9%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(9), true),
                new TaxDefinitionSeed("IN_GST18_IGST_18", "GST 18% IGST 18%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(18), true),
                new TaxDefinitionSeed("IN_GST28_CGST_14", "GST 28% CGST 14%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(14), true),
                new TaxDefinitionSeed("IN_GST28_SGST_14", "GST 28% SGST 14%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(14), true),
                new TaxDefinitionSeed("IN_GST28_IGST_28", "GST 28% IGST 28%", TaxDefinitionKind.TAX, TaxValueType.PERCENT, BigDecimal.valueOf(28), true)
        );
    }

    private List<TaxRuleSeed> buildIndiaTaxRuleSeeds(String sellerRegionCode, LocalDate effectiveFrom) {
        List<TaxRuleSeed> seeds = new ArrayList<>();
        addIndiaRuleTriplet(seeds, DEFAULT_TAX_CLASS_CODE, "IN_RESTAURANT_CGST_2_5", "IN_RESTAURANT_SGST_2_5", "IN_RESTAURANT_IGST_5", sellerRegionCode, effectiveFrom);
        addIndiaRuleTriplet(seeds, "GST_5", "IN_GST5_CGST_2_5", "IN_GST5_SGST_2_5", "IN_GST5_IGST_5", sellerRegionCode, effectiveFrom);
        addIndiaRuleTriplet(seeds, "GST_12", "IN_GST12_CGST_6", "IN_GST12_SGST_6", "IN_GST12_IGST_12", sellerRegionCode, effectiveFrom);
        addIndiaRuleTriplet(seeds, "GST_18", "IN_GST18_CGST_9", "IN_GST18_SGST_9", "IN_GST18_IGST_18", sellerRegionCode, effectiveFrom);
        addIndiaRuleTriplet(seeds, "GST_28", "IN_GST28_CGST_14", "IN_GST28_SGST_14", "IN_GST28_IGST_28", sellerRegionCode, effectiveFrom);
        addIndiaRuleTriplet(seeds, "GST_RESTAURANT_PREMISES_18", "IN_GST18_CGST_9", "IN_GST18_SGST_9", "IN_GST18_IGST_18", sellerRegionCode, effectiveFrom);
        return seeds;
    }

    private void addIndiaRuleTriplet(List<TaxRuleSeed> seeds,
                                     String taxClassCode,
                                     String cgstDefinitionCode,
                                     String sgstDefinitionCode,
                                     String igstDefinitionCode,
                                     String sellerRegionCode,
                                     LocalDate effectiveFrom) {
        if (sellerRegionCode != null) {
            seeds.add(new TaxRuleSeed(taxClassCode, cgstDefinitionCode, INDIA_COUNTRY_CODE, sellerRegionCode, TaxSupplyScope.INTRA_STATE, effectiveFrom, 1, 100));
            seeds.add(new TaxRuleSeed(taxClassCode, sgstDefinitionCode, INDIA_COUNTRY_CODE, sellerRegionCode, TaxSupplyScope.INTRA_STATE, effectiveFrom, 2, 100));
        }
        seeds.add(new TaxRuleSeed(taxClassCode, igstDefinitionCode, INDIA_COUNTRY_CODE, null, TaxSupplyScope.INTER_STATE, effectiveFrom, 1, 90));
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
        TaxRegistration defaultRegistration = taxRegistrationRepository
                .findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(accessibleRestaurantId)
                .orElse(null);
        Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(accessibleRestaurantId).orElse(null);
        String sellerCountryCode = resolveSellerCountryCode(restaurant, defaultRegistration);
        String sellerRegionCode = resolveSellerRegionCode(restaurant, defaultRegistration);
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
                    .filter(rule -> isRuleMatch(rule, taxClass, componentAmount, buyerContext, sellerCountryCode, sellerRegionCode))
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
        String sellerRegionCode = resolveSellerRegionCode(restaurant, null);
        TaxRegistration registration = new TaxRegistration();
        registration.setRestaurantId(restaurantId);
        registration.setSchemeCode("GST");
        registration.setRegistrationNumber(restaurant.getGstNumber().trim());
        registration.setLegalName(restaurant.getName());
        registration.setCountryCode(resolveSellerCountryCode(restaurant, null));
        registration.setRegionCode(sellerRegionCode);
        registration.setPlaceOfBusiness(buildPlaceOfBusiness(restaurant));
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
                                TaxBuyerContext buyerContext,
                                String sellerCountryCode,
                                String sellerRegionCode) {
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
        String supplyCountry = normalizeCountryCode(firstNonBlank(
                buyerContext == null ? null : buyerContext.placeOfSupplyCountryCode(),
                buyerContext == null ? null : buyerContext.buyerCountryCode()
        ));
        String supplyRegion = normalizeRegionCode(supplyCountry, firstNonBlank(
                buyerContext == null ? null : buyerContext.placeOfSupplyRegionCode(),
                buyerContext == null ? null : buyerContext.buyerRegionCode()
        ));
        String normalizedSellerCountry = normalizeCountryCode(sellerCountryCode);
        String normalizedSellerRegion = normalizeRegionCode(normalizedSellerCountry, sellerRegionCode);

        if (rule.getCountryCode() != null) {
            if (!normalizeCountryCode(rule.getCountryCode()).equals(supplyCountry)) {
                return false;
            }
        }
        if (rule.getRegionCode() != null) {
            if (!normalizeRegionCode(rule.getCountryCode(), rule.getRegionCode()).equals(supplyRegion)) {
                return false;
            }
        }
        if (!matchesSupplyScope(rule.getSupplyScope(), normalizedSellerCountry, normalizedSellerRegion, supplyCountry, supplyRegion)) {
            return false;
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

    private String normalizeCountryInput(String country) {
        String normalized = normalizeCountryCode(country);
        if (normalized != null) {
            return normalized;
        }
        throw new AppException("Invalid country code", HttpStatus.BAD_REQUEST);
    }

    private LocalDate currentIndianFinancialYearStart() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() >= 4
                ? LocalDate.of(today.getYear(), 4, 1)
                : LocalDate.of(today.getYear() - 1, 4, 1);
    }

    private String buildIndiaDatasetCode(LocalDate effectiveFrom) {
        int startYear = effectiveFrom.getYear();
        int endYear = (startYear + 1) % 100;
        return INDIA_DATASET_PREFIX + startYear + "_" + String.format("%02d", endYear);
    }

    private String resolveSellerCountryCode(Restaurant restaurant, TaxRegistration registration) {
        String registrationCountry = registration == null ? null : registration.getCountryCode();
        String normalizedRegistrationCountry = normalizeCountryCode(registrationCountry);
        if (normalizedRegistrationCountry != null) {
            return normalizedRegistrationCountry;
        }

        String restaurantCountry = restaurant == null ? null : restaurant.getCountry();
        String normalizedRestaurantCountry = normalizeCountryCode(restaurantCountry);
        if (normalizedRestaurantCountry != null) {
            return normalizedRestaurantCountry;
        }

        if (restaurant != null && trimToNull(restaurant.getGstNumber()) != null) {
            return INDIA_COUNTRY_CODE;
        }
        return null;
    }

    private String resolveSellerRegionCode(Restaurant restaurant, TaxRegistration registration) {
        String countryCode = resolveSellerCountryCode(restaurant, registration);
        String registrationRegion = registration == null ? null : registration.getRegionCode();
        String normalizedRegistrationRegion = normalizeRegionCode(countryCode, registrationRegion);
        if (normalizedRegistrationRegion != null) {
            return normalizedRegistrationRegion;
        }

        String restaurantRegion = restaurant == null ? null : restaurant.getState();
        String normalizedRestaurantRegion = normalizeRegionCode(countryCode, restaurantRegion);
        if (normalizedRestaurantRegion != null) {
            return normalizedRestaurantRegion;
        }

        String gstNumber = restaurant == null ? null : trimToNull(restaurant.getGstNumber());
        if (INDIA_COUNTRY_CODE.equals(countryCode) && gstNumber != null && gstNumber.length() >= 2) {
            String stateCode = gstNumber.substring(0, 2);
            return stateCode.chars().allMatch(Character::isDigit) ? stateCode : null;
        }
        return null;
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

    private String normalizeCountryCode(String value) {
        String normalized = normalizeCodeNullable(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "INDIA", "IN" -> INDIA_COUNTRY_CODE;
            default -> normalized;
        };
    }

    private String normalizeRegionCode(String countryCode, String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalizedCountry = normalizeCountryCode(countryCode);
        if (!INDIA_COUNTRY_CODE.equals(normalizedCountry)) {
            return normalizeCode(trimmed);
        }

        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (upper.length() == 2 && upper.chars().allMatch(Character::isDigit)) {
            return upper;
        }

        String canonical = upper
                .replace('&', ' ')
                .replaceAll("[^A-Z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");

        return switch (canonical) {
            case "JAMMU AND KASHMIR", "JAMMU KASHMIR" -> "01";
            case "HIMACHAL PRADESH" -> "02";
            case "PUNJAB" -> "03";
            case "CHANDIGARH" -> "04";
            case "UTTARAKHAND", "UTTRAKHAND" -> "05";
            case "HARYANA" -> "06";
            case "DELHI", "NEW DELHI", "NCT OF DELHI" -> "07";
            case "RAJASTHAN" -> "08";
            case "UTTAR PRADESH" -> "09";
            case "BIHAR" -> "10";
            case "SIKKIM" -> "11";
            case "ARUNACHAL PRADESH" -> "12";
            case "NAGALAND" -> "13";
            case "MANIPUR" -> "14";
            case "MIZORAM" -> "15";
            case "TRIPURA" -> "16";
            case "MEGHALAYA" -> "17";
            case "ASSAM" -> "18";
            case "WEST BENGAL" -> "19";
            case "JHARKHAND" -> "20";
            case "ODISHA", "ORISSA" -> "21";
            case "CHHATTISGARH", "CHATTISGARH" -> "22";
            case "MADHYA PRADESH" -> "23";
            case "GUJARAT" -> "24";
            case "DADRA AND NAGAR HAVELI AND DAMAN AND DIU", "DADRA AND NAGAR HAVELI", "DAMAN AND DIU", "DAMAN DIU" -> "26";
            case "MAHARASHTRA", "MAHARASTRA" -> "27";
            case "KARNATAKA" -> "29";
            case "GOA" -> "30";
            case "LAKSHADWEEP", "LAKSHADWEEP ISLANDS" -> "31";
            case "KERALA" -> "32";
            case "TAMIL NADU" -> "33";
            case "PUDUCHERRY", "PONDICHERRY" -> "34";
            case "ANDAMAN AND NICOBAR ISLANDS", "ANDAMAN NICOBAR", "ANDAMAN NICOBAR ISLANDS" -> "35";
            case "TELANGANA" -> "36";
            case "ANDHRA PRADESH" -> "37";
            case "LADAKH" -> "38";
            default -> normalizeCode(trimmed);
        };
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

    private boolean matchesSupplyScope(TaxSupplyScope supplyScope,
                                       String sellerCountryCode,
                                       String sellerRegionCode,
                                       String supplyCountryCode,
                                       String supplyRegionCode) {
        if (supplyScope == null || supplyScope == TaxSupplyScope.ANY) {
            return true;
        }
        if (sellerCountryCode == null || supplyCountryCode == null || sellerRegionCode == null || supplyRegionCode == null) {
            return false;
        }
        boolean sameCountry = sellerCountryCode.equals(supplyCountryCode);
        boolean sameRegion = sellerRegionCode.equals(supplyRegionCode);
        return switch (supplyScope) {
            case INTRA_STATE -> sameCountry && sameRegion;
            case INTER_STATE -> sameCountry && !sameRegion;
            case ANY -> true;
        };
    }

    private String buildPlaceOfBusiness(Restaurant restaurant) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, restaurant.getAddressLine1());
        appendPart(builder, restaurant.getAddressLine2());
        appendPart(builder, restaurant.getCity());
        appendPart(builder, restaurant.getState());
        appendPart(builder, restaurant.getPincode());
        return trimToNull(builder.toString());
    }

    private void appendPart(StringBuilder builder, String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(trimmed);
    }

    private String ruleSeedKey(TaxRule rule) {
        return ruleSeedKey(
                rule.getRestaurantId(),
                rule.getTaxDefinitionId(),
                rule.getTaxClassId(),
                rule.getCountryCode(),
                rule.getRegionCode(),
                rule.getSupplyScope(),
                rule.getSequenceNo()
        );
    }

    private String ruleSeedKey(Long restaurantId,
                               Long taxDefinitionId,
                               Long taxClassId,
                               String countryCode,
                               String regionCode,
                               TaxSupplyScope supplyScope,
                               Integer sequenceNo) {
        return restaurantId
                + "|" + taxDefinitionId
                + "|" + taxClassId
                + "|" + normalizeCountryCode(countryCode)
                + "|" + normalizeRegionCode(countryCode, regionCode)
                + "|" + (supplyScope == null ? TaxSupplyScope.ANY.name() : supplyScope.name())
                + "|" + sequenceNo;
    }

    private record RuleWithDefinition(TaxRule rule, TaxDefinition definition) {
    }

    private record ComponentTaxResult(BigDecimal taxableAmount,
                                      BigDecimal taxAmount,
                                      BigDecimal feeAmount,
                                      BigDecimal grandTotal,
                                      List<AppliedTaxComponent> appliedTaxes) {
    }

    private record TaxClassSeed(String code, String name, String description, boolean exempt) {
    }

    private record TaxDefinitionSeed(String code,
                                     String displayName,
                                     TaxDefinitionKind kind,
                                     TaxValueType valueType,
                                     BigDecimal defaultValue,
                                     boolean recoverable) {
    }

    private record TaxRuleSeed(String taxClassCode,
                               String taxDefinitionCode,
                               String countryCode,
                               String regionCode,
                               TaxSupplyScope supplyScope,
                               LocalDate validFrom,
                               Integer sequenceNo,
                               Integer priority) {
        private TaxCalculationMode calculationMode() {
            return TaxCalculationMode.EXCLUSIVE;
        }

        private TaxCompoundMode compoundMode() {
            return TaxCompoundMode.BASE_ONLY;
        }
    }

    private static final class SeedCounter {
        private int taxClassesCreated;
        private int taxClassesUpdated;
        private int taxDefinitionsCreated;
        private int taxDefinitionsUpdated;
        private int taxRulesCreated;
        private int taxRulesUpdated;
        private boolean defaultRegistrationCreated;
    }
}
